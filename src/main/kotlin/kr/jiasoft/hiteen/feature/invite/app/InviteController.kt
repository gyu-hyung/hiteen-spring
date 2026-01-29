package kr.jiasoft.hiteen.feature.invite.app

import io.swagger.v3.oas.annotations.Operation
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.invite.dto.InviteDeferredIssueResponse
import kr.jiasoft.hiteen.feature.invite.dto.InviteDeferredResolveResponse
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/invite")
class InviteController (
    private val inviteService: InviteService,
    private val inviteDeferredService: InviteDeferredService,
 ){

    @Operation(summary = "디퍼드 딥링크 토큰 발급", description = "초대코드를 Install Referrer에 담기 위한 토큰을 발급합니다.")
    @PostMapping("/deferred/issue")
    suspend fun issueDeferredToken(
        @RequestParam("code") code: String,
    ): ResponseEntity<ApiResult<InviteDeferredIssueResponse>> {
        val token = inviteDeferredService.issueToken(code)
        return ResponseEntity.ok(ApiResult.success(InviteDeferredIssueResponse(token)))
    }

    @Operation(summary = "디퍼드 딥링크 토큰 복원", description = "Install Referrer로 전달된 토큰을 초대코드로 복원합니다.")
    @GetMapping("/deferred/resolve")
    suspend fun resolveDeferredToken(
        @RequestParam("token") token: String,
        @AuthenticationPrincipal(expression = "user") user: UserEntity?,
    ): ResponseEntity<ApiResult<InviteDeferredResolveResponse>> {
        val code = inviteDeferredService.resolveToken(token, user?.id)
            ?: return ResponseEntity.badRequest().body(ApiResult.failure("invalid token"))
        return ResponseEntity.ok(ApiResult.success(InviteDeferredResolveResponse(code)))
    }


//    @PostMapping("/{targetUid}")
//    suspend fun createInvite(@AuthenticationPrincipal(expression = "user") user: UserEntity, @Parameter(description = "초대 대상 UUID") @PathVariable targetUid: UUID) {
//        ResponseEntity.ok(ApiResult.success(inviteService.giveInviteExp(user.id, targetUid)))
//    }


}