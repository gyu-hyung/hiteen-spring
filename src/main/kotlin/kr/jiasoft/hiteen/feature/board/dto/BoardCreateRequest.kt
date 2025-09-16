package kr.jiasoft.hiteen.feature.board.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class BoardCreateRequest(
    @field:NotBlank @field:Size(max = 200)
    val subject: String,
    @field:NotBlank
    val content: String,
    @field:Size(max = 50)
    val category: String,
    val link: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val status: String,
    val address: String,
    val detailAddress: String,
)