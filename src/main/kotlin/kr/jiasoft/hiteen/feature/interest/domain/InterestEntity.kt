package kr.jiasoft.hiteen.feature.interest.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("interests")
data class InterestEntity (
    @Id val id : Long? = null,
    val topic: String? = null,
    val category: String? = null,
    val status: String? = null,
    val createdId: Long? = null,
    val createdAt: OffsetDateTime? = null,
    val updatedId: Long? = null,
    val updatedAt: OffsetDateTime? = null,
    val deletedId: Long? = null,
    val deletedAt: OffsetDateTime? = null
)