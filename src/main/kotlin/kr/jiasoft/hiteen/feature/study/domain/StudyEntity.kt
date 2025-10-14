package kr.jiasoft.hiteen.feature.study.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("study")
data class StudyEntity(

    @Id
    val id: Long = 0L,

    val uid: String = UUID.randomUUID().toString(),                // 학습 세션 고유 식별자 (optional)
    val userId: Long,                       // 학습한 사용자 ID
    val seasonId: Long,                     // 시즌 ID
    val studyItems: String? = null,         // 학습 단어 리스트(JSON or CSV 문자열)
    val givePoint: Long = 0,                    // 보상 포인트
    val status: Long = 1,                       // 상태 (예: 0=준비, 1=진행중, 2=완료)
    val completeDate: OffsetDateTime? = null, // 학습 완료 시간
    val prep: Long = 0,                         // 준비 단어 수 / 학습량 등
    val prepPoint: Long = 0,                    // 준비 단계 포인트
    val prepDate: OffsetDateTime? = null,   // 준비 단계 완료일
    val createdAt: OffsetDateTime? = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime? = null,
    val deletedAt: OffsetDateTime? = null
)
