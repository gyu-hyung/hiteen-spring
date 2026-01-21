package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

data class AdminExpActionResponse(
    val actionCode: String,
    val description: String,
    val points: Int,
    val dailyLimit: Int?,
    val enabled: Boolean,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime?,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val updatedAt: OffsetDateTime?,
)

data class AdminExpActionCreateRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val actionCode: String?,

    @field:NotBlank
    @field:Size(max = 255)
    val description: String?,

    val points: Int? = 0,
    val dailyLimit: Int? = null,
    val enabled: Boolean? = true,
)

data class AdminExpActionUpdateRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val description: String?,

    val points: Int? = 0,
    val dailyLimit: Int? = null,
    val enabled: Boolean? = true,
)
