package kr.jiasoft.hiteen.admin.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "경험치 지급/차감")
data class AdminExpGiveRequest (
    val type : String? = "CREDIT",
    val amount: Int? = 0,
    val memo: String? = "",
    val receivers: List<String>? = emptyList(),
)