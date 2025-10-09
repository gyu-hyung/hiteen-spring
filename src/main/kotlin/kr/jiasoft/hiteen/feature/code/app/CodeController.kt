package kr.jiasoft.hiteen.feature.code.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.reactive.awaitSingle
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.code.dto.CodeRequest
import kr.jiasoft.hiteen.feature.code.dto.CodeWithAssetResponse
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux

@Tag(name = "Code", description = "공통 코드 관리 API")
@RestController
@RequestMapping("/api/codes")
@SecurityRequirement(name = "bearerAuth")
class CodeController(
    private val codeService: CodeService
) {

    @Operation(summary = "코드 단일 등록", description = "단일 코드 항목을 등록합니다. (파일 첨부 가능)")
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun createCode(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "코드 등록 요청 DTO") codeRequest: CodeRequest,
        @Parameter(description = "첨부할 파일") @RequestPart(name = "file", required = false) file: FilePart?
    ): ResponseEntity<ApiResult<CodeWithAssetResponse>> {
        val saved = codeService.createCode(user.id, codeRequest, file)
        return ResponseEntity.ok(ApiResult.success(saved))
    }


    @Operation(summary = "코드 수정", description = "특정 코드를 수정합니다. (파일 첨부 가능)")
    @PostMapping("/{id}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun updateCode(
        @Parameter(description = "수정할 코드 ID") @PathVariable id: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "코드 수정 요청 DTO") codeRequest: CodeRequest,
        @Parameter(description = "첨부할 파일") @RequestPart(name = "file", required = false) file: FilePart?
    ): ResponseEntity<ApiResult<CodeWithAssetResponse>> {
        val updated = codeService.updateCode(user.id, id, codeRequest, file)
        return ResponseEntity.ok(ApiResult.success(updated))
    }


    @Operation(summary = "코드 삭제", description = "특정 코드를 삭제합니다.")
    @DeleteMapping("/{id}")
    suspend fun deleteCode(
        @Parameter(description = "삭제할 코드 ID") @PathVariable id: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Unit>> {
        codeService.deleteCode(user.id, id)
        return ResponseEntity.ok(ApiResult.success(Unit))
    }


    @Operation(summary = "코드 그룹 생성 (파일 첨부 가능)",description = "파일 첨부를 통해 특정 그룹의 여러 개의 코드 항목을 생성합니다.")
    @PostMapping("/group/{group}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun createCodes(
        @Parameter(description = "코드 그룹명") @PathVariable group: String,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "첨부할 파일들") @RequestPart(name = "files", required = false) filesFlux: Flux<FilePart>?,
    ): ResponseEntity<ApiResult<List<Map<String, Any>>>> {
        val files: List<FilePart> = filesFlux?.collectList()?.awaitSingle().orEmpty()
        val saved = codeService.createCodesWithFiles(group, user.id, files)
        return ResponseEntity.ok(
            ApiResult.success(
                saved.map { mapOf("id" to it.id, "code" to it.code) }
            )
        )
    }


    @Operation(summary = "코드 그룹 조회", description = "특정 그룹에 속한 코드 목록을 조회합니다.")
    @GetMapping
    suspend fun listCodes(
        @Parameter(description = "조회할 코드 그룹명") @RequestParam group: String?
    ): ResponseEntity<ApiResult<List<CodeWithAssetResponse>>> {
        return ResponseEntity.ok(ApiResult.success(codeService.listCodesByGroup(group)))
    }
}