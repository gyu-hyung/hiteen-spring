package kr.jiasoft.hiteen.feature.cash.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "현재 보유 캐시")
data class CashBalanceResponse(
    @field:Schema(description = "현재 보유 캐시")
    val cash: Int,
)
