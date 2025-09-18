package kr.jiasoft.hiteen.feature.relationship.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "Follow", description = "팔로우 관련 API")
@RestController
@RequestMapping("/api/follows")
@SecurityRequirement(name = "bearerAuth")   // 🔑 JWT 인증 필요
class FollowController(
    private val followService: FollowService
) {

    //TODO UserSummary -> UserResponse 관심사 필요해서
    @Operation(summary = "내가 팔로우하는 목록", description = "현재 로그인한 사용자가 팔로우하고 있는 사용자 목록을 조회합니다.")
    @GetMapping("/following")
    suspend fun listFollowing(
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ) = ResponseEntity.ok(ApiResult(true, followService.listFollowing(user)))


    //TODO UserSummary -> UserResponse 관심사 필요해서
    @Operation(summary = "나를 팔로우하는 목록", description = "현재 로그인한 사용자를 팔로우하는 사용자 목록을 조회합니다.")
    @GetMapping("/followers")
    suspend fun listFollowers(
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ) = ResponseEntity.ok(ApiResult(true, followService.listFollowers(user)))


    @Operation(summary = "내가 보낸 팔로우 요청", description = "아직 수락되지 않은 내가 보낸 팔로우 요청들을 조회합니다.")
    @GetMapping("/requests/outgoing")
    suspend fun outgoing(
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ) = ResponseEntity.ok(ApiResult(true, followService.listOutgoing(user)))


    @Operation(summary = "내가 받은 팔로우 요청", description = "아직 수락하지 않은 내가 받은 팔로우 요청들을 조회합니다.")
    @GetMapping("/requests/incoming")
    suspend fun incoming(
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ) = ResponseEntity.ok(ApiResult(true, followService.listIncoming(user)))


    @Operation(summary = "팔로우 요청 보내기", description = "현재 로그인한 사용자가 특정 사용자에게 팔로우 요청을 보냅니다.")
    @PostMapping("/request/{userUid}")
    suspend fun request(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "팔로우 요청 대상 사용자 UID") @PathVariable userUid: String
    ) = ResponseEntity.ok(ApiResult(true, followService.request(user, userUid)))


    //TODO : 맞팔로우 플래그
    @Operation(summary = "팔로우 요청 승인", description = "받은 팔로우 요청을 승인합니다.")
    @PostMapping("/accept/{userUid}")
    suspend fun accept(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "팔로우 요청 보낸 사용자 UID") @PathVariable userUid: String
    ) = ResponseEntity.ok(ApiResult(true, followService.accept(user, userUid)))


    @Operation(summary = "팔로우 요청 거절", description = "받은 팔로우 요청을 거절합니다.")
    @DeleteMapping("/reject/{userUid}")
    suspend fun reject(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "팔로우 요청 보낸 사용자 UID") @PathVariable userUid: String
    ) = ResponseEntity.ok(ApiResult(true, followService.reject(user, userUid)))


    @Operation(summary = "팔로우 요청 취소", description = "내가 보낸 팔로우 요청을 취소합니다.")
    @DeleteMapping("/cancel/{userUid}")
    suspend fun cancel(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "취소할 팔로우 요청 대상 UID") @PathVariable userUid: String
    ) = ResponseEntity.ok(ApiResult(true, followService.cancel(user, userUid)))


    @Operation(summary = "언팔로우", description = "이미 팔로우한 사용자를 언팔로우합니다.")
    @DeleteMapping("/unfollow/{userUid}")
    suspend fun unfollow(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "언팔로우할 사용자 UID") @PathVariable userUid: String
    ) = ResponseEntity.ok(ApiResult(true, followService.unfollow(user, userUid)))


    @Operation(summary = "팔로워 강제 제거", description = "현재 사용자를 팔로우 중인 사용자를 강제로 팔로워 목록에서 제거합니다.")
    @DeleteMapping("/remove-follower/{userUid}")
    suspend fun removeFollower(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "강제 제거할 팔로워 UID") @PathVariable userUid: String
    ) = ResponseEntity.ok(ApiResult(true, followService.removeFollower(user, userUid)))
}
