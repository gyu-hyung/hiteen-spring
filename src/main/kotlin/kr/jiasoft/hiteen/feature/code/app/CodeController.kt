package kr.jiasoft.hiteen.feature.code.app

import kotlinx.coroutines.reactive.awaitSingle
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.code.dto.CodeWithAssetResponse
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux


@RestController
@RequestMapping("/api/codes")
class CodeController(
    private val codeService: CodeService
) {
    /** 공통 코드 그룹 + 파일 첨부 생성 */
    @PostMapping("/{group}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun createCodes(
        @PathVariable group: String,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestPart(name = "files", required = false) filesFlux: Flux<FilePart>?,
    ): ResponseEntity<ApiResult<List<Map<String, Any>>>> {
        val files: List<FilePart> = filesFlux?.collectList()?.awaitSingle().orEmpty()
        val saved = codeService.createCodesWithFiles(group, user.id, files)
        return ResponseEntity.ok(
            ApiResult.success(
                saved.map { mapOf("id" to it.id, "code" to it.code) }
            )
        )
    }

    /** 공통 코드 그룹 조회 */
    @GetMapping("/{group}")
    suspend fun listCodes(@PathVariable group: String): ResponseEntity<ApiResult<List<CodeWithAssetResponse>>> {
        return ResponseEntity.ok(ApiResult.success(codeService.listCodesByGroup(group)))
    }
}