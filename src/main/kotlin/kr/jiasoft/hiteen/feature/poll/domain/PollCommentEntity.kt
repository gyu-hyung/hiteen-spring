package kr.jiasoft.hiteen.feature.poll.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("poll_comments")
data class PollCommentEntity (
    @Id
    val id: Long = 0,
    val pollId: Long,
    val uid: UUID = UUID.randomUUID(),
    val parentId: Long? = null,
    val content: String,
    val replyCount: Int = 0,
    val reportCount: Int = 0,
    val createdId: Long,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime? = null,
    val deletedAt: OffsetDateTime? = null,
)