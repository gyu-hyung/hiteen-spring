package kr.jiasoft.hiteen.feature.user.dto

data class PublicUser(
    val uid: String,
    val username: String,
    val nickname: String?,
    val phone: String?,
    val address: String?,
    val detailAddress: String?,
    val mood: String?,
    val tier: String?,
    val assetUid: String?
)