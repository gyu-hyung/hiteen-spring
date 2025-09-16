package kr.jiasoft.hiteen.feature.board.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import kr.jiasoft.hiteen.feature.user.dto.UserSummary
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID


@Table("board_comments")
data class BoardCommentResponse(
    @Id
    @JsonIgnore
    val id: Long,
    val uid: UUID,
    val content: String,
    val replyCount: Int = 0,
    val likeCount: Long = 0,
    val likedByMe: Boolean = false,
    val parentUid: UUID? = null,
    @JsonIgnore
    val createdId: Long,
    val createdAt: OffsetDateTime,
    val user: UserSummary,
)