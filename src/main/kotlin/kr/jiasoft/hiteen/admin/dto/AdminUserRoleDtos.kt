package kr.jiasoft.hiteen.admin.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

@Schema(description = "어드민 권한 지정/해제 요청")
data class AdminUserRoleUpdateRequest(
    @field:NotBlank
    @field:Size(min = 4, max = 20)
    val role: String, // ADMIN | USER

    /** user uid */
    @field:NotBlank
    val uid: String,
)

@Schema(description = "어드민 권한 지정/해제 응답")
data class AdminUserRoleUpdateResponse(
    val id: Long,
    val uid: String,
    val role: String,
    val updatedId: Long?,
    val updatedAt: OffsetDateTime?,
)

