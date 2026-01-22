package kr.jiasoft.hiteen.admin.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "캐시 지급/차감 DTO")
data class AdminCashGiveRequest (
    @param:Schema(description = "지급구분(지급/차감)", example = "CREDIT")
    val type : String? = "CREDIT",

    @param:Schema(description = "금액", example = "1000")
    val amount: Int? = 0,

    @param:Schema(description = "지급/차감 사유", example = "이벤트 당첨 지급")
    val memo: String? = "",

    @param:Schema(description = "지급회원", example = "List<'01000000000','01000000001'>")
    val receivers: List<String>? = emptyList(),
)

