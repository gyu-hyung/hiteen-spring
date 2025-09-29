package kr.jiasoft.hiteen.feature.report.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.user.dto.UserSummary
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "신고 응답 DTO")
data class ReportResponse(

    @field:Schema(description = "신고 ID", example = "1")
    val id: Long,

    @field:Schema(description = "신고자 UID", example = "550e8400-e29b-41d4-a716-446655440000")
    val userUid: UUID,

    @field:Schema(description = "신고 대상 UID", example = "550e8400-e29b-41d4-a716-446655440001")
    val targetUid: UUID?,

    @field:Schema(description = "신고자 요약 정보")
    val user: UserSummary?,

    @field:Schema(description = "신고 대상자 요약 정보")
    val target: UserSummary?,

    @field:Schema(description = "신고 구분 (BOARD / COMMENT / POLL / POLL_COMMENT)", example = "COMMENT")
    val type: String,

    @field:Schema(description = "컨텐츠 모델명", example = "BoardComment")
    val reportableType: String?,

    @field:Schema(description = "컨텐츠 PK", example = "1001")
    val reportableId: Long?,

    @field:Schema(description = "신고 사유", example = "욕설/비방")
    val reason: String?,

    @field:Schema(description = "신고 사진 UID", example = "e7d8f690-9c3e-11ee-b9d1-0242ac120002")
    val photoUid: String?,

    @field:Schema(description = "상태 (0=대기, 1=완료)", example = "0")
    val status: Int,

    @field:Schema(description = "답변 내용", example = "조치 완료")
    val answer: String?,

    @field:Schema(description = "답변 일시", example = "2025-09-26T10:15:30+09:00")
    val answerAt: OffsetDateTime?,

    @field:Schema(description = "조치 메모", example = "해당 유저에게 경고 처리함")
    val memo: String?,

    @field:Schema(description = "신고 일시", example = "2025-09-26T09:00:00+09:00")
    @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val createdAt: OffsetDateTime,

    @field:Schema(description = "수정 일시", example = "2025-09-26T11:00:00+09:00")
    val updatedAt: OffsetDateTime?,

    @field:Schema(description = "삭제 일시", example = "2025-09-26T12:00:00+09:00")
    val deletedAt: OffsetDateTime?
)


//@Schema(description = "신고 요청 DTO")
//data class ReportRequest(
//
//    @field:Schema(description = "신고 대상 회원 번호", example = "42")
//    val targetUid: UUID,
//
//    @field:Schema(description = "신고 구분", example = "COMMENT")
//    val type: String,
//
//    @field:Schema(description = "컨텐츠 모델명", example = "BoardComment")
//    val reportableType: String? = null,
//
//    @field:Schema(description = "컨텐츠 PK", example = "1001")
//    val reportableId: Long? = null,
//
//    @field:Schema(description = "신고 사유", example = "욕설/비방")
//    val reason: String? = null,
//
//    @field:Schema(description = "신고 사진 UID", example = "e7d8f690-9c3e-11ee-b9d1-0242ac120002")
//    val photoUid: String? = null
//)

@Schema(description = "신고 요청 DTO")
data class ReportRequest(

    @field:Schema(description = "신고 대상 회원 UID", example = "550e8400-e29b-41d4-a716-446655440000")
    val targetUid: UUID? = null,

    @field:Schema(description = "신고 구분 (BOARD / COMMENT / POLL / POLL_COMMENT)", example = "COMMENT")
    val type: String,

    @field:Schema(description = "신고 대상 UID 또는 ID(BOARD / COMMENT / POLL_COMMENT)", example = "550e8400-e29b-41d4-a716-446655440111")
    val targetContentUid: UUID? = null,

    @field:Schema(description = "신고 대상 UID 또는 ID(POLL)", example = "101")
    val targetContentId: Long? = null,

    @field:Schema(description = "신고 사유", example = "욕설/비방")
    val reason: String? = null,

    @field:Schema(description = "신고 사진 UID", example = "e7d8f690-9c3e-11ee-b9d1-0242ac120002")
    val photoUid: String? = null
)