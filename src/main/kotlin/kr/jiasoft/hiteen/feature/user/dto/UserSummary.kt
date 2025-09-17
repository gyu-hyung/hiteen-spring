package kr.jiasoft.hiteen.feature.user.dto

import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kotlin.String


data class UserSummary(
    val uid: String,
    val username: String,
    val nickname: String?,
    val phone: String?,
    val address: String?,
    val detailAddress: String?,
    val mood: String?,
    val mbti: String?,
    val tier: String?,
    val assetUid: String?,
    val isFriend: Boolean? = false,
) {
    companion object {
        fun from(user: UserEntity, isFriend: Boolean? = false): UserSummary =
            UserSummary(
                uid = user.uid.toString(),
                username = user.username,
                nickname = user.nickname,
                phone = user.phone,
                address = user.address,
                detailAddress = user.detailAddress,
                mood = user.mood,
                mbti = user.mbti,
                tier = user.tier,
                assetUid = user.assetUid?.toString(),
                isFriend = isFriend
            )
        fun empty() = UserSummary(
            uid = "",
            username = "",
            nickname = null,
            phone = null,
            address = null,
            detailAddress = null,
            mood = null,
            mbti = null,
            tier = null,
            assetUid = null,
            isFriend = false
        )
    }

}