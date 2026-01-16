package kr.jiasoft.hiteen.admin.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AdminExpActionResponse(
    val actionCode: String,
    val description: String,
    val points: Int,
    val dailyLimit: Int?,
    val enabled: Boolean,
    val createdAt: String?,
    val updatedAt: String?,
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
