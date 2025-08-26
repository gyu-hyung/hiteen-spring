package kr.jiasoft.hiteen.feature.board.dto

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class BoardCommentCreateRequest(
    @field:NotBlank val content: String,
    val parentUid: UUID? = null,
)