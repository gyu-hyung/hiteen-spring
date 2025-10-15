package kr.jiasoft.hiteen.feature.notification.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "푸시 알림 응답")
data class PushNotificationResponse(
    @field:Schema(description = "푸시 ID")
    val id: Long,
    @field:Schema(description = "푸시 코드")
    val code: String?,
    @field:Schema(description = "제목")
    val title: String?,
    @field:Schema(description = "메세지")
    val message: String?,
    @field:Schema(description = "성공 수")
    val success: Int,
    @param:Schema(description = "생성 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val createdAt: OffsetDateTime?
)
