package kr.jiasoft.hiteen.feature.poll.domain

import io.r2dbc.postgresql.codec.Json
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("polls")
data class PollEntity (
    @Id
    val id: Long = 0,
    val question: String,
    val photo: UUID? = null,
    val colorStart: String? = null,
    val colorEnd: String? = null,
    val voteCount: Int = 0,
    val commentCount: Int = 0,
    val reportCount: Int = 0,
    val allowComment: Int = 0,
    val status: String = "ACTIVE",//PollStatus.ACTIVE
    val createdId: Long,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime? = null,
    val deletedAt: OffsetDateTime? = null,
)
