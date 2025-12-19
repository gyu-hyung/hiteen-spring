package kr.jiasoft.hiteen.feature.gift.dto.client

data class GiftishowApiResponse <T> (
    val code: String,
    val message: String?,
    val result: T?
)