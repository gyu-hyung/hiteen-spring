package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "관리자 SMS 목록 아이템")
data class AdminSmsListResponse(
    val id: Long,
    val title: String?,
    val content: String?,
    val callback: String?,
    val total: Long?,
    val success: Long?,
    val failure: Long?,
    val createdId: Long?,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val updatedAt: OffsetDateTime?,
)

@Schema(description = "관리자 SMS 상세")
data class AdminSmsDetailResponse(
    val sms: AdminSmsListResponse,
    val authLogs: List<AdminSmsAuthResponse>,
)

@Schema(description = "관리자 SMS 인증 로그")
data class AdminSmsAuthResponse(
    val id: Long,
    val smsId: Long,
    val phone: String,
    val code: String,
    val status: String,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val updatedAt: OffsetDateTime?,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val deletedAt: OffsetDateTime?,
)

