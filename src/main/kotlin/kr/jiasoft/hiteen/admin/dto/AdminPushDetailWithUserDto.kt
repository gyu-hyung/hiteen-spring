package kr.jiasoft.hiteen.admin.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "푸시 상세(회원정보 포함)")
data class AdminPushDetailWithUserDto(
    val id: Long,
    val pushId: Long?,
    val userId: Long?,
    val userName: String?,
    val deviceOs: String?,
    val deviceToken: String?,
    val phone: String?,
    val multicastId: String?,
    val messageId: String?,
    val registrationId: String?,
    val error: String?,
    val success: Int,
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?,
    val deletedAt: OffsetDateTime?,
)