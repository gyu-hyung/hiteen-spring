package kr.jiasoft.hiteen.feature.relationship.app

import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/follows")
class FollowController(
    private val followService: FollowService
) {

    /** 내가 팔로우하고 있는 목록 */
    @GetMapping("/following")
    suspend fun listFollowing(@AuthenticationPrincipal(expression = "user") user: UserEntity) =
        ResponseEntity.ok(ApiResult(true, followService.listFollowing(user)))


    /** 나를 팔로우하는 목록  */
    @GetMapping("/followers")
    suspend fun listFollowers(@AuthenticationPrincipal(expression = "user") user: UserEntity) =
        ResponseEntity.ok(ApiResult(true, followService.listFollowers(user)))


    /** 내가 보낸 팔로우 요청 */
    @GetMapping("/requests/outgoing")
    suspend fun outgoing(@AuthenticationPrincipal(expression = "user") user: UserEntity) =
        ResponseEntity.ok(ApiResult(true, followService.listOutgoing(user)))


    /** 내가 받은 팔로우 요청 */
    @GetMapping("/requests/incoming")
    suspend fun incoming(@AuthenticationPrincipal(expression = "user") user: UserEntity) =
        ResponseEntity.ok(ApiResult(true, followService.listIncoming(user)))


    /** 팔로우 요청 보내기: me -> {uid} */
    @PostMapping("/request")
    suspend fun request(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestParam uid: String
    ) = ResponseEntity.ok(ApiResult(true, followService.request(user, uid)))


    /** 받은 팔로우 요청 승인 */
    @PostMapping("/accept")
    suspend fun accept(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestParam uid: String
    ) = ResponseEntity.ok(ApiResult(true, followService.accept(user, uid)))


    /** 받은 팔로우 요청 거절 */
    @PostMapping("/reject")
    suspend fun reject(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestParam uid: String
    ) = ResponseEntity.ok(ApiResult(true, followService.reject(user, uid)))


    /** 내가 보낸 팔로우 요청 취소 */
    @PostMapping("/cancel")
    suspend fun cancel(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestParam uid: String
    ) = ResponseEntity.ok(ApiResult(true, followService.cancel(user, uid)))


    /** 언팔로우 (이미 팔로우 상태 끊기) */
    @PostMapping("/unfollow")
    suspend fun unfollow(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestParam uid: String
    ) = ResponseEntity.ok(ApiResult(true, followService.unfollow(user, uid)))


    /** 나를 팔로우하는 사람 강제 제거 */
    @PostMapping("/remove-follower")
    suspend fun removeFollower(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestParam uid: String
    ) = ResponseEntity.ok(ApiResult(true, followService.removeFollower(user, uid)))

}
