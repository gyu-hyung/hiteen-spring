package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

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

    @get:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yy.MM.dd HH:mm")
    val createdDate: String? = createdAt?.format(DateTimeFormatter.ofPattern("yy.MM.dd HH:mm")),
    @get:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yy.MM.dd HH:mm")
    val updatedDate: String? = updatedAt?.format(DateTimeFormatter.ofPattern("yy.MM.dd HH:mm")),
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
