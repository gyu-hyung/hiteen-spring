package kr.jiasoft.hiteen.feature.notification.app

import io.swagger.v3.oas.annotations.Operation
import kr.jiasoft.hiteen.common.dto.ApiPageCursor
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.notification.dto.PushNotificationResponse
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.web.bind.annotation.RequestParam

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationService: NotificationService
) {

    @Operation(
        summary = "푸시 알림 내역 조회",
        description = """
            푸시, 채팅, 공지, 이벤트, 친구 요청 등 앱 내 알림 내역을 조회합니다.
        | FRIEND_REQUEST | 친구 요청 💌
        | FRIEND_ACCEPT  | 친구 요청 승인 💌
        | FOLLOW_REQUEST | 새로운 팔로우 👀
        | FOLLOW_ACCEPT  | 새로운 팔로우 👀
        | NEW_POST       | 새 글 등록 ✍️
        | PIN_REGISTER   | 핀 등록 알림 📍
        | BOARD_COMMENT  | 틴스토리 댓글 알림 💬
        | VOTE_COMMENT   | 틴투표 댓글 알림 💬
        | CHAT_MESSAGE   | 새로운 채팅 💬
        """
    )
    @GetMapping("/push")
    suspend fun getPushNotifications(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestParam(required = false) cursor: Long?,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(required = false) code: PushTemplate?
    ): ResponseEntity<ApiResult<ApiPageCursor<PushNotificationResponse>>> {
        val result = notificationService.getPushNotifications(user.id, cursor, limit, code)
        return ResponseEntity.ok(ApiResult.success(result))
    }



}
