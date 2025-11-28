package kr.jiasoft.hiteen.feature.gift_v2.dto.client.voucher

data class GiftishowVoucherDetailResult(
    val couponInfoList: List<GiftishowVoucherDetailDto>,
    val resCode: String?,
    val resMsg: String?
)
