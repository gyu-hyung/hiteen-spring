package kr.jiasoft.hiteen.feature.notification.app

import io.swagger.v3.oas.annotations.Operation
import kr.jiasoft.hiteen.common.dto.ApiPageCursor
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.notification.dto.PushNotificationResponse
import kr.jiasoft.hiteen.feature.notification.dto.PushTemplateGroupResponse
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
import kr.jiasoft.hiteen.feature.push.domain.PushTemplateGroup
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestParam

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationService: NotificationService,
    private val notificationTemplateService: NotificationTemplateService,
) {

    @Operation(
        summary = "푸시 알림 내역 조회",
        description = """
            앱 내 알림 내역(친구 요청, 팔로우, 새 글 등록, 댓글 등)을 조회합니다.
            채팅 메시지(CHAT_MESSAGE)는 기본 목록에서 제외됩니다.
            특정 코드나 그룹을 지정하여 필터링 조회가 가능합니다.
        | FRIEND_REQUEST | 친구 요청 💌
        | FRIEND_ACCEPT  | 친구 요청 승인 💌
        | FOLLOW_REQUEST | 새로운 팔로우 👀
        | FOLLOW_ACCEPT  | 팔로우 수락 🥰
        | NEW_POST       | 새 글 등록 ✍️
        | PIN_REGISTER   | 핀 등록 알림 📍
        | BOARD_COMMENT  | 틴스토리 댓글 알림 💬
        | VOTE_COMMENT   | 틴투표 댓글 알림 💬
        """
    )
    @GetMapping("/push")
    suspend fun getPushNotifications(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestParam(required = false) cursor: Long?,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(required = false) code: PushTemplate?,
        @RequestParam(required = false) group: PushTemplateGroup?,
    ): ResponseEntity<ApiResult<ApiPageCursor<PushNotificationResponse>>> {
        val result = notificationService.getPushNotifications(user.id, cursor, limit, code, group)
        return ResponseEntity.ok(ApiResult.success(result))
    }

    @DeleteMapping
    suspend fun deletePushNotification(
        @AuthenticationPrincipal(expression = "user") user: UserEntity, @RequestParam id: Long? = null, @RequestParam(required = false) all: Boolean = false
    ): ResponseEntity<ApiResult<String>> {
        notificationService.delete(user.id, id, all)
        return ResponseEntity.ok(ApiResult.success("성공"))
    }

    @Operation(
        summary = "푸시 템플릿 목록(그룹)",
        description = """
            PushTemplate 종류가 많아 그룹으로 묶어 조회합니다.
            - group 파라미터가 없으면 전체 그룹 반환
            - group 파라미터가 있으면 해당 그룹만 반환
        """
    )
    @GetMapping("/push/templates")
    suspend fun getPushTemplatesGrouped(
        @RequestParam(required = false) group: PushTemplateGroup?,
    ): ResponseEntity<ApiResult<List<PushTemplateGroupResponse>>> {
        val result = notificationTemplateService.getPushTemplatesGrouped(group)
        return ResponseEntity.ok(ApiResult.success(result))
    }

}
