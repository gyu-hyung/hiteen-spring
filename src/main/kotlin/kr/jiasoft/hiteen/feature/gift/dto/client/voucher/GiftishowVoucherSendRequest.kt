package kr.jiasoft.hiteen.feature.gift.dto.client.voucher

data class GiftishowVoucherSendRequest(
    val goodsCode: String,
    val orderNo: String? = null,
    val mmsTitle: String,
    val mmsMsg: String,
//    val callbackNo: String,
    val phoneNo: String,
    val trId: String,
    val revInfoYn: String = "N",
    val revInfoDate: String? = null,//yyyyMMdd
    val revInfoTime: String? = null,//HHmm
//    val templateId: String? = null,
//    val bannerId: String? = null,
//    val userId: String,
    val gubun: String = "I"
)
