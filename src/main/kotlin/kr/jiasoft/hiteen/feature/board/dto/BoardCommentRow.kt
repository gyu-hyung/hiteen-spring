package kr.jiasoft.hiteen.feature.board.dto

import org.springframework.data.relational.core.mapping.Column
import java.time.OffsetDateTime
import java.util.UUID

data class BoardCommentRow(
    val uid: UUID,
    val content: String?,

    @Column("created_at")
    val createdAt: OffsetDateTime?,

    @Column("created_id")
    val createdId: Long,

    @Column("reply_count")
    val replyCount: Int,

    @Column("like_count")
    val likeCount: Long,

    @Column("liked_by_me")
    val likedByMe: Boolean,

    @Column("parent_uid")
    val parentUid: UUID?
)
