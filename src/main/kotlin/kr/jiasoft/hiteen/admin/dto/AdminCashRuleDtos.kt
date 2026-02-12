package kr.jiasoft.hiteen.admin.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

@Schema(name = "AdminCashRuleCreateRequest")
data class AdminCashRuleCreateRequest(
    @field:NotBlank
    @field:Size(max = 50)
    val actionCode: String,

    @field:Min(-1_000_000)
    @field:Max(1_000_000)
    val amount: Int,

    @field:Min(0)
    val dailyCap: Int? = null,

    @field:Min(0)
    val cooldownSec: Int? = null,

    @field:Size(max = 200)
    val description: String? = null,
)

@Schema(name = "AdminCashRuleUpdateRequest")
data class AdminCashRuleUpdateRequest(
    @field:Min(-1_000_000)
    @field:Max(1_000_000)
    val amount: Int? = null,

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

@Schema(name = "AdminCashRuleResponse")
data class AdminCashRuleResponse(
    val id: Long,
    val actionCode: String,
    val amount: Int,
    val dailyCap: Int?,
    val cooldownSec: Int?,
    val description: String?,
    val deletedAt: OffsetDateTime?,
)

