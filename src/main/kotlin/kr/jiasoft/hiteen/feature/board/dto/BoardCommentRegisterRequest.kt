package kr.jiasoft.hiteen.feature.board.dto

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class BoardCommentRegisterRequest(
    val boardUid: UUID? = null,
    val commentUid: UUID? = null,
    @field:NotBlank
    val content: String? = null,
    val parentUid: UUID? = null,
)