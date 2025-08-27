package kr.jiasoft.hiteen.feature.board.dto

import jakarta.validation.constraints.NotBlank

data class BoardCommentUpdateRequest(
    @field:NotBlank(message = "content must not be blank")
    val content: String
)