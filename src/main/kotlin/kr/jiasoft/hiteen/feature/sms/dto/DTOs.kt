package kr.jiasoft.hiteen.feature.sms.dto

data class AligoResponse(
    val result_code: String,  // "1" 처럼 문자열일 수도 있음
    val message: String?,
    val msg_id: String?,
    val success_cnt: Int?,
    val error_cnt: Int?,
    val msg_type: String?
) {
    fun isSuccess(): Boolean = result_code == "1" || result_code == "success" || result_code == "0"
}
