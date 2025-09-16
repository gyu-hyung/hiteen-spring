package kr.jiasoft.hiteen.feature.poll.domain

import io.r2dbc.postgresql.codec.Json
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("polls")
data class PollEntity (
    @Id
    val id: Long = 0,
    val question: String,
    val photo: String? = null,
    val selects: Json,
    val colorStart: String? = null,
    val colorEnd: String? = null,
    val voteCount: Int = 0,
    val commentCount: Int = 0,
    val reportCount: Int = 0,
    val allowComment: Int = 0,
    val status: String,
    val createdAt: OffsetDateTime,
    val createdId: Long,
    val updatedAt: OffsetDateTime? = null,
    val deletedAt: OffsetDateTime? = null,
)
