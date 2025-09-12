package kr.jiasoft.hiteen.feature.poll.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("poll_comments")
data class PollCommentEntity (
    @Id
    val id: Long? = null,
    val pollId: Long? = null,
    val uid: UUID = UUID.randomUUID(),
    val parentId: Long? = null,
    val content: String? = null,
    val replyCount: Int = 0,
    val reportCount: Int = 0,
    val createdId: Long? = null,
    val createdAt: OffsetDateTime? = null,
    val updatedAt: OffsetDateTime? = null,
    val deletedAt: OffsetDateTime? = null,
)