package kr.jiasoft.hiteen.feature.board.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.user.dto.UserSummary
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "게시판 댓글 응답 DTO")
@Table("board_comments")
data class BoardCommentResponse(

    @Id
    @JsonIgnore
    @param:Schema(description = "댓글 PK (내부용)", example = "1", hidden = true)
    val id: Long,

    @param:Schema(description = "댓글 UID", example = "550e8400-e29b-41d4-a716-446655440000")
    val uid: UUID,

    @param:Schema(description = "댓글 내용", example = "좋은 글이네요! 감사합니다.")
    val content: String,

    @param:Schema(description = "게시글 UID", example = "550e8400-e29b-41d4-a716-446655440000")
    val boardUid: UUID,

    @param:Schema(description = "게시글 내용", example = "맛집정보 공유합니다.맛집정보 공유합니다.맛집정보 공유합니다.맛집정보 공유합니다.맛집정보 공유합니다.")
    val boardContent: String? = null,

    @param:Schema(description = "대댓글 개수", example = "2")
    val replyCount: Int = 0,

    @param:Schema(description = "게시글에 달린 댓글 개수")
    val boardCommentCount: Int = 0,

    @param:Schema(description = "좋아요 개수", example = "15")
    val likeCount: Long = 0,

    @param:Schema(description = "내가 좋아요 눌렀는지 여부", example = "true")
    val likedByMe: Boolean = false,

    @param:Schema(description = "부모 댓글 UID (대댓글일 경우)", example = "550e8400-e29b-41d4-a716-446655441111")
    val parentUid: UUID? = null,

    @JsonIgnore
    @param:Schema(description = "작성자 ID (내부용)", example = "1001", hidden = true)
    val createdId: Long,

    @param:Schema(description = "작성 일시", example = "2025.09.18 10:15")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val createdAt: OffsetDateTime,

    @param:Schema(description = "작성자 정보 요약")
    val user: UserSummary?,
)
