package kr.jiasoft.hiteen.feature.poll.domain

import io.r2dbc.postgresql.codec.Json
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("polls")
data class PollEntity (
    @Id
    val id: Long? = null,
    val question: String? = null,
    val photo: String? = null,
    val selects: Json? = null,
    val colorStart: String? = null,
    val colorEnd: String? = null,
    val voteCount: Int? = null,
    val commentCount: Int? = null,
    val reportCount: Int? = null,
    val allowComment: Int? = null,
    val status: String? = null,
    val createdAt: OffsetDateTime? = null,
    val createdId: Long? = null,
    val updatedAt: OffsetDateTime? = null,
    val deletedAt: OffsetDateTime? = null,
)
