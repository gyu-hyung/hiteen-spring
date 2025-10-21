package kr.jiasoft.hiteen.feature.asset.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.asset.dto.AssetResponse
import kr.jiasoft.hiteen.feature.asset.dto.toResponse
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.core.io.FileSystemResource
import org.springframework.http.*
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import java.lang.IllegalArgumentException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

@Tag(name = "Asset", description = "파일 업로드/다운로드/조회 API")
@RestController
@RequestMapping("/api/assets")
@SecurityRequirement(name = "bearerAuth")   // 🔑 JWT 인증 필요
class AssetController(
    private val assetService: AssetService
) {

    @Operation(
        summary = "파일 업로드",
        description = "단일 파일을 업로드합니다.",
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "multipart/form-data 형식의 파일 업로드",
            required = true,
            content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)]
        )
    )
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun upload(
        @Parameter(description = "업로드할 파일") @RequestPart("file") file: FilePart,
        @Parameter(description = "원본 파일명 (선택)") @RequestPart(name = "originFileName", required = false) originFileName: String?,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<AssetResponse>>
    = ResponseEntity.ok(ApiResult.success(assetService.upload(file, originFileName, currentUserId = user.id)))

    @Operation(
        summary = "여러 파일 업로드",
        description = "여러 개의 파일을 한 번에 업로드합니다.",
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "multipart/form-data 형식의 파일 업로드",
            required = true,
            content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)]
        )
    )
    @PostMapping("/batch", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun uploadBatch(
        @Parameter(description = "업로드할 파일 목록") @RequestPart(name = "files", required = false) filesFlux: Flux<FilePart>?,
        @Parameter(description = "원본 파일명 목록") @RequestPart(name = "originFileNames", required = false) originFileNames: List<String>?,
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<ApiResult<List<AssetResponse>>> {
        val flux = filesFlux ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "files or file part is required")

        val files: List<FilePart> = flux.collectList().awaitSingle()
        if (files.isEmpty()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "no files")

        return ResponseEntity.ok(ApiResult.success(assetService.uploadAll(files, currentUserId = user.id, originFileNames = originFileNames)))
    }

    @Operation(summary = "단건 조회", description = "특정 파일 메타데이터를 조회합니다.")
    @GetMapping("/{uid}")
    suspend fun getOne(
        @Parameter(description = "파일 UID") @PathVariable uid: UUID
    ): ResponseEntity<AssetResponse> {
        val e = assetService.get(uid) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(e.toResponse())
    }

    @Operation(summary = "파일 목록 조회", description = "등록된 파일들을 간단 페이징으로 조회합니다.")
    @GetMapping
    suspend fun list(
        @Parameter(description = "조회 개수 (기본 20)") @RequestParam(defaultValue = "20") limit: Int,
        @Parameter(description = "조회 시작 offset (기본 0)") @RequestParam(defaultValue = "0") offset: Int
    ): ResponseEntity<ApiResult<List<AssetResponse>>> {
        val result = assetService.list(limit.coerceIn(1, 100), offset.coerceAtLeast(0))
            .map { it.toResponse() }
            .toList()

        return ResponseEntity.ok(ApiResult.success(result))
    }

    @Operation(
        summary = "파일 다운로드",
        description = "파일을 다운로드하며, 다운로드 횟수를 증가시킵니다.",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "파일 다운로드 성공",
                content = [Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)]
            )
        ]
    )
    @GetMapping("/{uid}/download")
    suspend fun download(
        @Parameter(description = "파일 UID") @PathVariable uid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<FileSystemResource> {
        assetService.increase(uid)
        val updated = assetService.findByUid(uid)?: throw IllegalArgumentException("존재하지않는 uid")

        val path = assetService.resolveFilePath(updated.filePath + updated.storeFileName)
        if (!assetService.existsFile(path)) return ResponseEntity.notFound().build()

        val resource = FileSystemResource(path)
        val mime = updated.type ?: MediaType.APPLICATION_OCTET_STREAM_VALUE
        val fileName = updated.originFileName
        val encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20")

        val headers = HttpHeaders().apply {
            contentType = MediaType.parseMediaType(mime)
            contentLength = resource.contentLength()
            add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''$encoded")
        }
        return ResponseEntity.ok()
            .headers(headers)
            .body(resource)
    }



    @GetMapping("/{uid}/view")
    suspend fun view(
        @PathVariable uid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<FileSystemResource> {
        val asset = assetService.findByUid(uid) ?: return ResponseEntity.notFound().build()

        val path = assetService.resolveFilePath(asset.filePath + asset.storeFileName)
        if (!assetService.existsFile(path)) return ResponseEntity.notFound().build()

        val resource = FileSystemResource(path)
        val mime = asset.type ?: MediaType.APPLICATION_OCTET_STREAM_VALUE

        val headers = HttpHeaders().apply {
            contentType = MediaType.parseMediaType(mime)
            contentLength = resource.contentLength()
            // ✅ inline으로 설정하면 브라우저가 바로 렌더링
            add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${asset.originFileName}\"")
        }

        return ResponseEntity.ok()
            .headers(headers)
            .body(resource)
    }


    @Operation(summary = "파일 삭제", description = "특정 파일을 소프트 삭제(메타데이터만 변경)합니다.")
    @DeleteMapping("/{uid}")
    suspend fun delete(
        @Parameter(description = "파일 UID") @PathVariable uid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<ApiResult<AssetResponse>> {
        val deleted = assetService.softDelete(uid, user.id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(ApiResult.success(deleted.toResponse()))
    }
}
