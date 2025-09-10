package kr.jiasoft.hiteen.feature.board.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.OffsetDateTime

data class BoardCreateRequest(
    @field:NotBlank @field:Size(max = 200)
    val subject: String,
    @field:NotBlank
    val content: String,
    @field:Size(max = 50)
    val category: String? = null,
    val link: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val status: String? = null,
    val address: String? = null,
    val detailAddress: String? = null,
)