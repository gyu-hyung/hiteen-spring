package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

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
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yy.MM.dd HH:mm:ss")
    val createdDate: String? = createdAt.format(DateTimeFormatter.ofPattern("yy.MM.dd HH:mm:ss")),
)

@Schema(description = "관리자 SMS 상세")
data class AdminSmsDetailResponse(
    val sms: AdminSmsListResponse,
    /** 인증문자(sms_auth) 로그 */
    val authLogs: List<AdminSmsAuthResponse> = emptyList(),

    /** 일반문자(sms_details) 로그 */
    val smsDetails: List<AdminSmsDetailLogResponse> = emptyList(),
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

@Schema(description = "관리자 SMS 발송 상세 로그")
data class AdminSmsDetailLogResponse(
    val id: Long,
    val smsId: Long,
    val phone: String,
    val success: Int,
    val error: String?,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val updatedAt: OffsetDateTime?,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val deletedAt: OffsetDateTime?,
)
