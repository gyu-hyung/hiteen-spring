package kr.jiasoft.hiteen.feature.notification.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "알림 아이템")
data class NotificationItem(
    @field:Schema(description = "UID", example = "uuid-1234")
    val uid: String?,

    @field:Schema(description = "메시지", example = "새로운 푸시 알림")
    val message: String?,

    @field:Schema(description = "생성일시", example = "2025-09-29 11:30:00")
    val createdAt: LocalDateTime?
)