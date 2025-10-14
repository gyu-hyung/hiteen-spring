package kr.jiasoft.hiteen.feature.notification.dto

import java.time.OffsetDateTime

data class PushNotificationResponse(
    val id: Long,
    val code: String?,
    val title: String?,
    val message: String?,
    val success: Int,
    val createdAt: OffsetDateTime?
)
