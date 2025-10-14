package kr.jiasoft.hiteen.feature.user.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("user_details")
data class UserDetailEntity (
    @Id
    val id: Long = 0,
    var userId: Long,
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

    val pushItems: String,

    val memo: String? = null

)