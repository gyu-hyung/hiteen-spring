package kr.jiasoft.hiteen.feature.interest.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("interest_user")
data class InterestUserEntity(
    @Id val id: Long = 0,
    val interestId: Long,
    val userId: Long,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime? = null
)