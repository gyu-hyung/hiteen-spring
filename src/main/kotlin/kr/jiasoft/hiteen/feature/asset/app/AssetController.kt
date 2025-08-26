package kr.jiasoft.hiteen.feature.asset.app


import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.asset.dto.AssetResponse
import kr.jiasoft.hiteen.feature.asset.dto.toResponse
import org.springframework.core.io.FileSystemResource
import org.springframework.http.*
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

@RestController
@RequestMapping("/api/assets")
class AssetController(
    private val assetService: AssetService
) {

    /** TODO 여러 파일 업로드 */

    /** 업로드 (multipart/form-data: file, originFileName[opt]) */
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun upload(
        @RequestPart("file") file: FilePart,
        @RequestPart(name = "originFileName", required = false) originFileName: String?,
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): AssetResponse {
        return assetService.upload(file, originFileName, currentUserId = user.id!!)
    }

    /** 단건 조회 (메타데이터) */
    @GetMapping("/{uid}")
    suspend fun getOne(@PathVariable uid: UUID): ResponseEntity<AssetResponse> {
        val e = assetService.get(uid) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(e.toResponse())
    }

    /** 목록 조회 (간단 페이징) */
    @GetMapping
    suspend fun list(
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): List<AssetResponse> {
        return assetService.list(limit.coerceIn(1, 100), offset.coerceAtLeast(0))
            .map { it.toResponse() }
            .toList()
    }

    /** 다운로드 (Content-Disposition + download_count 증가) */
    @GetMapping("/{uid}/download")
    suspend fun download(
        @PathVariable uid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<FileSystemResource> {
//        val updated = assetService.increaseDownload(uid, user.id!!)
        val updated = assetService.increaseDownload(uid, 13)
            ?: return ResponseEntity.notFound().build()

        val filePath = updated.filePath ?: return ResponseEntity.notFound().build()
        val path = assetService.resolveFilePath(filePath)
        if (!assetService.existsFile(path)) return ResponseEntity.notFound().build()

        val resource = FileSystemResource(path)
        val mime = updated.type ?: MediaType.APPLICATION_OCTET_STREAM_VALUE
        val fileName = (updated.originFileName ?: updated.storeFileName ?: "download")
        val encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20")

        val headers = HttpHeaders().apply {
            contentType = MediaType.parseMediaType(mime)
            contentLength = resource.contentLength()
            add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''$encoded")
            // 캐싱 정책 필요 시 여기에 추가
        }
        return ResponseEntity.ok()
            .headers(headers)
            .body(resource)
    }

    /** 소프트 삭제 (메타데이터만 deleted_*) */
    @DeleteMapping("/{uid}")
    suspend fun delete(
        @PathVariable uid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<AssetResponse> {
        val deleted = assetService.softDelete(uid, user.id!!) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(deleted.toResponse())
    }
}
