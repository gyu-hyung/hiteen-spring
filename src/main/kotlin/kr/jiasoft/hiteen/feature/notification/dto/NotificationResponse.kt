package kr.jiasoft.hiteen.feature.notification.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "알림 응답 DTO")
data class NotificationResponse(
    val push: NotificationItem?,
    val chat: NotificationItem?,
    val notice: NotificationItem?,
    val event: NotificationItem?,
    val gift: NotificationItem?,
    val friend: NotificationItem?
)
