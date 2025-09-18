package kr.jiasoft.hiteen.feature.board.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.util.UUID

@Schema(description = "게시글 댓글 등록 요청 DTO")
data class BoardCommentRegisterRequest(

    @param:Schema(
        description = "댓글을 작성할 게시글 UID",
        example = "550e8400-e29b-41d4-a716-446655440000",
        required = true
    )
    val boardUid: UUID,

    @param:Schema(
        description = "수정할 댓글 UID (등록 시 null, 수정 시 값 전달)",
        example = "550e8400-e29b-41d4-a716-446655440111"
    )
    val commentUid: UUID? = null,

    @field:NotBlank
    @param:Schema(
        description = "댓글 내용",
        example = "좋은 글이네요! 저도 같이 하고 싶어요.",
        required = true
    )
    val content: String,

    @param:Schema(
        description = "부모 댓글 UID (대댓글 작성 시 필요)",
        example = "550e8400-e29b-41d4-a716-446655440222"
    )
    val parentUid: UUID? = null,
)
