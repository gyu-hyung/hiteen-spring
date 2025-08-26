package kr.jiasoft.hiteen.feature.board.dto

import java.time.OffsetDateTime
import java.util.UUID

data class BoardCommentResponse(
    val uid: UUID,
    val content: String,
    val createdAt: OffsetDateTime?,
    val createdId: Long,
    val replyCount: Int,
    val likeCount: Long,
    val likedByMe: Boolean,
    val parentUid: UUID? = null,
)