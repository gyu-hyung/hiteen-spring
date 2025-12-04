package kr.jiasoft.hiteen.feature.gift_v2.dto.client.voucher

data class GiftishowVoucherDetailWrapperDto(
    val couponInfoList: List<GiftishowCouponInfoDto>,
    val resCode: String?,
    val resMsg: String?
)
