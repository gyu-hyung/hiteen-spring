package kr.jiasoft.hiteen.feature.giftishow.dto.send

data class GiftishowSendResponse(
    val code: String,
    val message: String?,
    val result: SendResult?
)
