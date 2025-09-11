package kr.jiasoft.hiteen.feature.interest.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("interest_user")
data class InterestUserEntity(
    @Id val id: Long? = null,
    val interestId: Long,
    val userId: Long,
    val createdAt: OffsetDateTime? = null,
    val updatedAt: OffsetDateTime? = null
)