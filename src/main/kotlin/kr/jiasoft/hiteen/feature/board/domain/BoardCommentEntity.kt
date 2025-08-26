package kr.jiasoft.hiteen.feature.board.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("board_comments")
data class BoardCommentEntity (
    @Id
    val id: Long? = null,
    val boardId: Long,
    val uid: UUID = UUID.randomUUID(),
    val parentId: Long? = null,
    val content: String,
    val replyCount: Int = 0,
    val reportCount: Int = 0,
    val createdId: Long,
    val createdAt: OffsetDateTime? = null,
    val updatedId: Long? = null,
    val updatedAt: OffsetDateTime? = null,
    val deletedId: Long? = null,
    val deletedAt: OffsetDateTime? = null,
)