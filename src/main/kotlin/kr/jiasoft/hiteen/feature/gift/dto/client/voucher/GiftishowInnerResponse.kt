package kr.jiasoft.hiteen.feature.gift.dto.client.voucher

data class GiftishowInnerResponse<T>(
    val code: String,
    val message: String?,
    val result: T?
)
