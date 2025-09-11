package kr.jiasoft.hiteen.feature.interest.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table
data class InterestUserResponse (
    @Id
    val id: Long,
    @JsonIgnore
    val userId: Long,
    val topic: String,
    val category: String,
    val status: String?,
    val userUid: UUID,
    val interestId: Long,
    val createdAt: OffsetDateTime,
)