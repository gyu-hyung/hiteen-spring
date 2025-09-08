package kr.jiasoft.hiteen.feature.relationship.app

import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.relationship.dto.FriendSearchItem
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/friends")
class FriendController(
    private val friendService: FriendService
) {

    /** 내 친구 목록 (수락됨) */
    @GetMapping
    suspend fun list(@AuthenticationPrincipal(expression = "user") user: UserEntity)
            = friendService.listFriends(user)


    /** 내가 보낸 대기중 요청 */
    @GetMapping("/requests/outgoing")
    suspend fun outgoing(@AuthenticationPrincipal(expression = "user") user: UserEntity)
            = friendService.listOutgoing(user)


    /** 내가 받은 대기중 요청 */
    @GetMapping("/requests/incoming")
    suspend fun incoming(@AuthenticationPrincipal(expression = "user") user: UserEntity)
            = friendService.listIncoming(user)


    /** 검색 (유저 uid/username/nickname/email) */
    @GetMapping("/search")
    suspend fun search(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestParam q: String,
        @RequestParam(required = false, defaultValue = "30") limit: Int
    ): ResponseEntity<ApiResult<List<FriendSearchItem>>> {
        val results = friendService.search(user, q, limit)
        return ResponseEntity.ok(
            ApiResult(
                success = true,
                data = results
            )
        )
    }

    /** 친구 요청 보내기: me -> {uid} */
    @PostMapping("/request")
    suspend fun request(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestParam uid: String
    ) {
        friendService.request(user, uid)
    }


    /** 받은 요청 수락: {uid} -> me */
    @PostMapping("/accept")
    suspend fun accept(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestParam uid: String
    ) {
        friendService.accept(user, uid)
    }


    /** 받은 요청 거절 */
    @PostMapping("/reject")
    suspend fun reject(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestParam uid: String
    ) {
        friendService.reject(user, uid)
    }


    /** 내가 보낸 요청 취소 */
    @PostMapping("/cancel")
    suspend fun cancel(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestParam uid: String
    ) {
        friendService.cancel(user, uid)
    }


    /** 친구 끊기 */
    @PostMapping("/unfriend")
    suspend fun unfriend(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestParam uid: String
    ) {
        friendService.unfriend(user, uid)
    }
}
