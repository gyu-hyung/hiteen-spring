package kr.jiasoft.hiteen.feature.gift.dto.client.voucher

data class GiftishowVoucherDetailResult(
    val couponInfoList: List<GiftishowVoucherDetailDto>,
    val resCode: String?,
    val resMsg: String?
)
