package kr.jiasoft.hiteen.feature.user.dto

data class UserDetailRequest(
    val deviceId: String? = null,
    val deviceOs: String? = null,
    val deviceVersion: String? = null,
    val deviceDetail: String? = null,
    val deviceToken: String? = null,
    val locationToken: String? = null,
    val aqnsToken: String? = null,
    val apiToken: String? = null,
    val agreeService: String? = null,
    val agreePrivacy: String? = null,
    val agreeFinance: String? = null,
    val agreeMarketing: String? = null,
    val pushService: String? = null,
    val pushMarketing: String? = null,
    val memo: String? = null
)

data class UserDetailResponse(
    val userId: Long,
    val deviceId: String?,
    val deviceOs: String?,
    val deviceVersion: String?,
    val deviceDetail: String?,
    val deviceToken: String?,
    val locationToken: String?,
    val aqnsToken: String?,
    val apiToken: String?,
    val agreeService: String?,
    val agreePrivacy: String?,
    val agreeFinance: String?,
    val agreeMarketing: String?,
    val pushService: String?,
    val pushMarketing: String?,
    val memo: String?
)
