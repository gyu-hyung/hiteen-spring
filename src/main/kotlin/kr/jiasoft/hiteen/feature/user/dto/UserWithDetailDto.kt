package kr.jiasoft.hiteen.feature.user.dto

data class UserWithDetailDto(
    val userId: Long,
    val phone: String?,
    val deviceOs: String?,
    val deviceToken: String?
)
