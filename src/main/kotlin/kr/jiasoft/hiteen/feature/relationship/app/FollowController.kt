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
    @PostMapping("/request/{userUid}")
    suspend fun request(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @PathVariable userUid: String
    ) = ResponseEntity.ok(ApiResult(true, followService.request(user, userUid)))


    /** 받은 팔로우 요청 승인
     * TODO : 맞팔로우 플래그
     * */
    @PostMapping("/accept/{userUid}")
    suspend fun accept(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @PathVariable userUid: String
    ) = ResponseEntity.ok(ApiResult(true, followService.accept(user, userUid)))


    /** 받은 팔로우 요청 거절 */
    @PostMapping("/reject/{userUid}")
    suspend fun reject(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @PathVariable userUid: String
    ) = ResponseEntity.ok(ApiResult(true, followService.reject(user, userUid)))


    /** 내가 보낸 팔로우 요청 취소 */
    @PostMapping("/cancel/{userUid}")
    suspend fun cancel(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @PathVariable userUid: String
    ) = ResponseEntity.ok(ApiResult(true, followService.cancel(user, userUid)))


    /** 언팔로우 (이미 팔로우 상태 끊기) */
    @PostMapping("/unfollow/{userUid}")
    suspend fun unfollow(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @PathVariable userUid: String
    ) = ResponseEntity.ok(ApiResult(true, followService.unfollow(user, userUid)))


    /** 나를 팔로우하는 사람 강제 제거 */
    @PostMapping("/remove-follower/{userUid}")
    suspend fun removeFollower(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @PathVariable userUid: String
    ) = ResponseEntity.ok(ApiResult(true, followService.removeFollower(user, userUid)))

}
