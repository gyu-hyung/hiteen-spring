package kr.jiasoft.hiteen.feature.interest.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("interests")
data class InterestEntity (
    @Id val id : Long = 0,
    val topic: String,
    val category: String,
    val status: String,
    val createdId: Long,
    val createdAt: OffsetDateTime,
    val updatedId: Long? = null,
    val updatedAt: OffsetDateTime? = null,
    val deletedId: Long? = null,
    val deletedAt: OffsetDateTime? = null
)