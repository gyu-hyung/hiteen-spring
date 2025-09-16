package kr.jiasoft.hiteen.feature.user.domain

import org.springframework.data.relational.core.mapping.Table

@Table("user_details")
data class UserDetailEntity (
    val id: Long,
    val userId: Long,
    val deviceId: String,
    val deviceOs: String,
    val deviceVersion: String,
    val deviceDetail: String,
    val deviceToken: String,
    val locationToken: String,
    val aqnsToken: String,
    val apiToken: String,
    val agreeService: String,
    val agreePrivacy: String,
    val agreeFinance: String,
    val agreeMarketing: String,
    val pushService: String,
    val pushMarketing: String,
    val memo: String,
)