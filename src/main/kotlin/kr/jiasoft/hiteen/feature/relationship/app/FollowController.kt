package kr.jiasoft.hiteen.feature.relationship.app

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/follows")
class FollowController(
    private val followService: FollowService
) {

    /** 내 친구 목록 (수락됨) */
    @GetMapping
    suspend fun list(@AuthenticationPrincipal(expression = "user") user: UserEntity)
            = followService.listFriends(user)

    /** 내가 보낸 대기중 요청 */
    @GetMapping("/requests/outgoing")
    suspend fun outgoing(@AuthenticationPrincipal(expression = "user") user: UserEntity)
            = followService.listOutgoing(user)

    /** 내가 받은 대기중 요청 */
    @GetMapping("/requests/incoming")
    suspend fun incoming(@AuthenticationPrincipal(expression = "user") user: UserEntity)
            = followService.listIncoming(user)

    /** 검색 (유저 uid/username/nickname/email) */
    @GetMapping("/search")
    suspend fun search(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestParam q: String,
        @RequestParam(required = false, defaultValue = "30") limit: Int
    ) = followService.search(user, q, limit)

    /** 친구 요청 보내기: me -> {uid} */
    @PostMapping("/request")
    suspend fun request(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestParam uid: String
    ) {
        followService.request(user, uid)
    }

    /** 받은 요청 수락: {uid} -> me */
    @PostMapping("/accept")
    suspend fun accept(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestParam uid: String
    ) {
        followService.accept(user, uid)
    }

    /** 받은 요청 거절 */
    @PostMapping("/reject")
    suspend fun reject(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestParam uid: String
    ) {
        followService.reject(user, uid)
    }

    /** 내가 보낸 요청 취소 */
    @PostMapping("/cancel")
    suspend fun cancel(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestParam uid: String
    ) {
        followService.cancel(user, uid)
    }

    /** 친구 끊기 */
    @PostMapping("/unfollow")
    suspend fun unfriend(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestParam uid: String
    ) {
        followService.unfriend(user, uid)
    }
}
