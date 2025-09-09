package kr.jiasoft.hiteen.feature.user.dto

import kr.jiasoft.hiteen.feature.user.domain.UserEntity


data class UserSummary(
    val uid: String,
    val username: String,
    val nickname: String?,
    val phone: String?,
    val address: String?,
    val detailAddress: String?,
    val mood: String?,
    val tier: String?,
    val assetUid: String?
) {
    companion object {
        fun from(user: UserEntity): UserSummary =
            UserSummary(
                uid = user.uid.toString(),
                username = user.username,
                nickname = user.nickname,
                phone = user.phone,
                address = user.address,
                detailAddress = user.detailAddress,
                mood = user.mood,
                tier = user.tier,
                assetUid = user.assetUid?.toString()
            )
        fun empty() = UserSummary(
            uid = "",
            username = "",
            nickname = null,
            phone = null,
            address = null,
            detailAddress = null,
            mood = null,
            tier = null,
            assetUid = null
        )
    }

}