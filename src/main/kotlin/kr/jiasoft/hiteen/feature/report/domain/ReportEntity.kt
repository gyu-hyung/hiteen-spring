package kr.jiasoft.hiteen.feature.report.domain

import kr.jiasoft.hiteen.feature.report.dto.ReportResponse
import kr.jiasoft.hiteen.feature.user.dto.UserSummary
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("reports")
data class ReportEntity(

    @Id
    val id: Long = 0L, // 신고번호 (PK)

    val userId: Long, // 신고자 회원번호

    val targetId: Long? = null, // 신고대상 회원번호

    val type: String, // 신고 구분 (댓글/글/실시간/채팅 …)

    val reportableType: String? = null, // 컨텐츠 모델명

    val reportableId: Long? = null, // 컨텐츠 PK

    val reason: String? = null, // 신고 사유

    val photoUid: String? = null, // 신고 사진 UID

    val status: Int = 0, // 상태: 대기(0), 완료(1)

    val answer: String? = null, // 답변내용

    val answerAt: OffsetDateTime? = null, // 답변일시

    val memo: String? = null, // 조치내용

    val createdAt: OffsetDateTime = OffsetDateTime.now(), // 신고일시

    val updatedAt: OffsetDateTime? = null, // 변경일시

    val deletedAt: OffsetDateTime? = null, // 삭제일시
)
fun ReportEntity.toResponse(
    userUid: UUID,
    targetUid: UUID?,
    userSummary: UserSummary?,
    targetSummary: UserSummary?
) = ReportResponse(
    id = id,
    userUid = userUid,
    targetUid = targetUid,
    user = userSummary,
    target = targetSummary,
    type = type,
    reportableType = reportableType,
    reportableId = reportableId,
    reason = reason,
    photoUid = photoUid,
    status = status,
    answer = answer,
    answerAt = answerAt,
    memo = memo,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)
