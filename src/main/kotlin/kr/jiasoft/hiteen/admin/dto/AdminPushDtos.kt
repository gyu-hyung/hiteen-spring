package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

@Schema(description = "관리자 푸시 목록 응답")
data class AdminPushListResponse(
    val id: Long,
    val type: String?,
    val code: String?,
    val title: String?,
    val message: String?,
    val total: Long,
    val success: Long,
    val failure: Long,
    val createdId: Long?,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime?,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val deletedAt: OffsetDateTime?,
)

@Schema(description = "관리자 푸시 상세(요약)")
data class AdminPushDetailResponse(
    val push: AdminPushListResponse,
    val details: List<AdminPushDetailItem>,
)

@Schema(description = "관리자 푸시 상세 항목")
data class AdminPushDetailItem(
    val id: Long,
    val pushId: Long?,
    val userId: Long?,
    val deviceOs: String?,
    val deviceToken: String?,
    val phone: String?,
    val messageId: String?,
    val error: String?,
    val success: Int,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime?,
)

@Schema(description = "관리자 푸시 생성 요청")
data class AdminPushCreateRequest(
    @field:Size(max = 50)
    val code: String? = null,

    @field:Size(max = 255)
    val title: String? = null,

    val message: String? = null,

    /**
     * silent, notification 등
     */
    @field:Size(max = 50)
    val type: String? = "notification",

    /**
     * 대상 userId 목록
     */
    val userIds: List<Long>,
)

@Schema(description = "관리자 푸시 목록 조회 조건")
data class AdminPushSearchParams(
    val page: Int = 1,
    val size: Int = 20,
    val order: String = "DESC",
    val searchType: String = "ALL", // ALL|CODE|TITLE|MESSAGE
    val search: String? = null,
    val status: String? = null, // ACTIVE|DELETED|ALL
)

@Schema(description = "관리자 푸시 상세 목록 조회 조건")
data class AdminPushDetailSearchParams(
    val page: Int = 1,

    val size: Int = 50,

     val success: String? = null, // ALL|SUCCESS|FAIL
)

@Schema(description = "관리자 푸시 소프트 삭제 응답")
data class AdminPushDeleteResponse(
    val id: Long,
    val deleted: Boolean,
)
