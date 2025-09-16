package kr.jiasoft.hiteen.feature.code.app

import kotlinx.coroutines.reactive.awaitSingle
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.code.dto.EmojiResponse
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
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
    /** 이모지 등록 */
    @PostMapping("/emojis", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun createEmojis(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestPart("files") filesFlux: Flux<FilePart>
    ): ResponseEntity<ApiResult<List<Map<String, Any>>>> {
        val files = filesFlux.collectList().awaitSingle()
        val saved = codeService.createEmojis(user.id, files)
        return ResponseEntity.ok(
            ApiResult.success(
                saved.map { mapOf("id" to it.id, "code" to it.code) }
            )
        )
    }


    /** 이모지 목록 조회 */
    @GetMapping("/emojis")
    suspend fun listEmojis(): ResponseEntity<ApiResult<List<EmojiResponse>>> {
        return ResponseEntity.ok(ApiResult.success(codeService.listEmojis()))
    }
}
