package kr.jiasoft.hiteen.feature.gift.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "선물 생성 요청")
data class GiftBuyRequest (

    @field:Schema(description = "상품코드 - Voucher")
    val goodsCode: String,

    @field:Schema(description = "수신자 uid")
    val receiveUserUid: UUID? = null,

    @field:Schema(description = "메모")
    val memo: String? = null,

)
