package kr.jiasoft.hiteen.feature.interest.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("interest_match_history")
data class InterestMatchHistoryEntity (
    @Id
    val id: Long = 0,
    val userId: Long,
    val targetId: Long,
    val status: String,
    val createdAt: OffsetDateTime,
)
