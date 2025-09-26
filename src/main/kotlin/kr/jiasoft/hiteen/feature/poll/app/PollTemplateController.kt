package kr.jiasoft.hiteen.feature.poll.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.poll.dto.PollTemplateRequest
import kr.jiasoft.hiteen.feature.poll.dto.PollTemplateResponse
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/poll-templates")
class PollTemplateController(
    private val pollTemplateService: PollTemplateService
) {

    // ---------------- 사용자 ----------------
    @Operation(summary = "투표 템플릿 목록 조회")
    @GetMapping
    suspend fun listTemplates(): ResponseEntity<ApiResult<List<PollTemplateResponse>>> {
        val templates = pollTemplateService.listActiveTemplates()
        return ResponseEntity.ok(ApiResult.success(templates))
    }

    @Operation(summary = "투표 템플릿 단건 조회")
    @GetMapping("/{id}")
    suspend fun getTemplate(@PathVariable id: Long): ResponseEntity<ApiResult<PollTemplateResponse>> {
        val template = pollTemplateService.getTemplate(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(ApiResult.success(template))
    }

    // ---------------- 관리자 ----------------
    @Operation(summary = "투표 템플릿 생성 (ADMIN 전용)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    suspend fun createTemplate(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "등록 투표 템플릿") req: PollTemplateRequest
    ): ResponseEntity<Long> {
        println("user.id = ${user.id}")
        val id = pollTemplateService.createTemplate(req)
        return ResponseEntity.ok(id)
    }


    @Operation(summary = "투표 템플릿 수정 (ADMIN 전용)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}")
    suspend fun updateTemplate(
        @PathVariable id: Long,
        @Parameter(description = "수정할 투표 템플릿") req: PollTemplateRequest
    ): ResponseEntity<Void> {
        pollTemplateService.updateTemplate(id, req)
        return ResponseEntity.ok().build()
    }

    @Operation(summary = "투표 템플릿 삭제 (ADMIN 전용, soft-delete)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    suspend fun deleteTemplate(@PathVariable id: Long): ResponseEntity<Void> {
        pollTemplateService.deleteTemplate(id)
        return ResponseEntity.ok().build()
    }
}
