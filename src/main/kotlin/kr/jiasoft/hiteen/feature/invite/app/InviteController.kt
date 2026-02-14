package kr.jiasoft.hiteen.feature.invite.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.invite.dto.InviteDeferredIssueResponse
import kr.jiasoft.hiteen.feature.invite.dto.InviteDeferredResolveResponse
import kr.jiasoft.hiteen.feature.invite.dto.InviteRankingResponse
import kr.jiasoft.hiteen.feature.invite.dto.InviteStatsResponse
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

    @Operation(summary = "초대 랭킹 조회", description = "추천을 많이 한 회원 순으로 랭킹을 조회합니다.")
    @GetMapping("/ranking")
    suspend fun getInviteRanking(
        @Parameter(description = "조회할 랭킹 수 (기본 10, 최대 100)")
        @RequestParam(defaultValue = "10") limit: Int,
    ): ResponseEntity<ApiResult<List<InviteRankingResponse>>> {
        val effectiveLimit = limit.coerceIn(1, 100)
        val ranking = inviteService.getInviteRanking(effectiveLimit)
        return ResponseEntity.ok(ApiResult.success(ranking))
    }

    @Operation(summary = "기간별 회원가입 통계 조회", description = "특정 기간 동안 회원가입한 유저 수를 조회합니다.")
    @GetMapping("/stats/join")
    suspend fun getJoinStats(
        @Parameter(description = "시작일시 (형식: yyyy-MM-dd HH:mm:ss, 예: 2025-01-01 00:00:00)")
        @RequestParam startDate: String,
        @Parameter(description = "종료일시 (형식: yyyy-MM-dd HH:mm:ss, 예: 2025-12-31 23:59:59)")
        @RequestParam endDate: String,
    ): ResponseEntity<ApiResult<InviteStatsResponse>> {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val zoneId = java.time.ZoneId.of("Asia/Seoul")
        val parsedStartDate = java.time.LocalDateTime.parse(startDate, formatter).atZone(zoneId).toOffsetDateTime()
        val parsedEndDate = java.time.LocalDateTime.parse(endDate, formatter).atZone(zoneId).toOffsetDateTime()
        val stats = inviteService.getJoinStats(parsedStartDate, parsedEndDate)
        return ResponseEntity.ok(ApiResult.success(stats))
    }


//    @PostMapping("/{targetUid}")
//    suspend fun createInvite(@AuthenticationPrincipal(expression = "user") user: UserEntity, @Parameter(description = "초대 대상 UUID") @PathVariable targetUid: UUID) {
//        ResponseEntity.ok(ApiResult.success(inviteService.giveInviteExp(user.id, targetUid)))
//    }


}