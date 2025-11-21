package kr.jiasoft.hiteen.feature.giftishow.dto.send

data class GiftishowSendRequest(
    val api_code: String = "0204",
    val custom_auth_code: String,
    val custom_auth_token: String,
    val dev_yn: String = "Y",
    val goods_code: String,
    val tr_id: String,
    val mms_msg: String,
    val mms_title: String,
    val callback_no: String,
    val phone_no: String,
    val user_id: String,
    val banner_id: String?,
    val template_id: String?,
    val gubun: String = "I"
)
