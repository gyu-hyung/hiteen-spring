package kr.jiasoft.hiteen.admin.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "관리자 신고 목록/상세 응답")
data class AdminReportResponse(
    @field:Schema(description = "신고 ID")
    val id: Long,

    @field:Schema(description = "신고자 UID")
    val userUid: UUID,

    @field:Schema(description = "신고자 닉네임")
    val userNickname: String?,

    @field:Schema(description = "신고 대상 UID")
    val targetUid: UUID?,

    @field:Schema(description = "신고 대상 닉네임")
    val targetNickname: String?,

    @field:Schema(description = "신고 구분")
    val type: String,

    @field:Schema(description = "컨텐츠 타입")
    val reportableType: String?,

    @field:Schema(description = "컨텐츠 ID")
    val reportableId: Long?,

    @field:Schema(description = "신고 사유")
    val reason: String?,

    @field:Schema(description = "신고 사진 UID")
    val photoUid: String?,

    @field:Schema(description = "상태 (0=대기, 1=처리, 2=반려)")
    val status: Int,

    @field:Schema(description = "관리자 답변")
    val answer: String?,

    @field:Schema(description = "답변 일시")
    val answerAt: OffsetDateTime?,

    @field:Schema(description = "조치 메모")
    val memo: String?,

    @field:Schema(description = "생성 일시")
    val createdAt: OffsetDateTime,

    @field:Schema(description = "수정 일시")
    val updatedAt: OffsetDateTime?,

    @field:Schema(description = "삭제 일시")
    val deletedAt: OffsetDateTime?,
)

@Schema(description = "관리자 신고 처리 요청")
data class AdminReportProcessRequest(
    @field:Schema(description = "처리 답변")
    val answer: String? = null,

    @field:Schema(description = "조치 메모")
    val memo: String? = null,
)


@Schema(description = "관리자 신고 반려 요청")
data class AdminReportRejectRequest(
    @field:Schema(description = "반려 답변")
    val answer: String? = null,

    @field:Schema(description = "반려 사유/메모")
    val memo: String? = null,
)
