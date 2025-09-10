package kr.jiasoft.hiteen.feature.board.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID


@Table("board_comments")
data class BoardCommentResponse(
    @Id
    @JsonIgnore
    val id: Long? = null,
    val uid: UUID? = null,
    val content: String? = null,
    val createdAt: OffsetDateTime? = null,
    val createdId: Long? = null,
    val replyCount: Int? = null,
    val likeCount: Long? = null,
    val likedByMe: Boolean? = null,
    val parentUid: UUID? = null,
)