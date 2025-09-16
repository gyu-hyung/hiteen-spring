package kr.jiasoft.hiteen.feature.interest.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("interests")
data class InterestEntity (
    @Id val id : Long = 0,
    val topic: String,
    val category: String,
    val status: String,
    @JsonIgnore
    val createdId: Long,
    val createdAt: OffsetDateTime,
    @JsonIgnore
    val updatedId: Long? = null,
    val updatedAt: OffsetDateTime? = null,
    @JsonIgnore
    val deletedId: Long? = null,
    val deletedAt: OffsetDateTime? = null
)