package kr.jiasoft.hiteen.feature.cash.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min

data class CashExchangeRequest(
    @field:Min(10)
    @field:Schema(description = "환전할 캐시", example = "200")
    val cash: Int
)

