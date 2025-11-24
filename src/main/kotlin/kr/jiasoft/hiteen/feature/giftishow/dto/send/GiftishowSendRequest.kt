package kr.jiasoft.hiteen.feature.giftishow.dto.send

data class GiftishowSendRequest(
    val api_code: String = "0204",
    val custom_auth_code: String,
    val custom_auth_token: String,
    val dev_yn: String = "Y",
    val goods_code: String,
    val order_no: String? = null,
    val mms_title: String,
    val mms_msg: String,
    val callback_no: String,
    val phone_no: String,
    val tr_id: String,
    val rev_info_yn: String = "N",
    val rev_info_time: String? = null,
    val template_id: String? = null,
    val banner_id: String? = null,
    val user_id: String,
    val gubun: String = "I"
)
