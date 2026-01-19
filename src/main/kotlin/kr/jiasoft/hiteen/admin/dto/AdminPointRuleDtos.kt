package kr.jiasoft.hiteen.admin.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

@Schema(name = "AdminPointRuleCreateRequest")
data class AdminPointRuleCreateRequest(
    @field:NotBlank
    @field:Size(max = 50)
    val actionCode: String,

    @field:Min(-1_000_000)
    @field:Max(1_000_000)
    val point: Int,

    @field:Min(0)
    val dailyCap: Int? = null,

    @field:Min(0)
    val cooldownSec: Int? = null,

    @field:Size(max = 200)
    val description: String? = null,
)

@Schema(name = "AdminPointRuleUpdateRequest")
data class AdminPointRuleUpdateRequest(
    @field:Min(-1_000_000)
    @field:Max(1_000_000)
    val point: Int? = null,

    @field:Min(0)
    val dailyCap: Int? = null,

    @field:Min(0)
    val cooldownSec: Int? = null,

    @field:Size(max = 200)
    val description: String? = null,

    /**
     * soft delete 된 정책을 복구하고 싶을 때 true
     */
    val restore: Boolean? = null,
)

@Schema(name = "AdminPointRuleResponse")
data class AdminPointRuleResponse(
    val id: Long,
    val actionCode: String,
    val point: Int,
    val dailyCap: Int?,
    val cooldownSec: Int?,
    val description: String?,
    val deletedAt: OffsetDateTime?,
)
