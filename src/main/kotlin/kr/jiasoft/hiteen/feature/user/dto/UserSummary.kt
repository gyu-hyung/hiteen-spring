package kr.jiasoft.hiteen.feature.user.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.user.domain.UserEntity

@Schema(description = "간단한 사용자 요약 정보")
data class UserSummary(

    @JsonIgnore
    @param:Schema(description = "사용자 ID", example = "1")
    val id: Long,

    @param:Schema(description = "사용자 고유 식별자 (UUID)", example = "6f9b90d6-96ca-49de-b9c2-b123e51ca7db")
    val uid: String,

    @param:Schema(description = "사용자 로그인 아이디", example = "hong123")
    val username: String,

    @param:Schema(description = "닉네임", example = "홍길동")
    val nickname: String?,

    @param:Schema(description = "주소", example = "서울특별시 강남구 테헤란로 123")
    val address: String?,

    @param:Schema(description = "상세 주소", example = "아파트 101동 202호")
    val detailAddress: String?,

    @param:Schema(description = "휴대폰 번호", example = "01012345678")
    val phone: String?,

    @param:Schema(description = "현재 기분/상태 메시지", example = "기분좋음")
    val mood: String?,

    @param:Schema(description = "현재 기분/상태 이모지", example = "E_001")
    val moodEmoji: String?,

    @param:Schema(description = "MBTI", example = "ENTP")
    val mbti: String?,

    @param:Schema(description = "현재 누적 경험치", example = "9999")
    val expPoints: Long,

    @param:Schema(description = "티어명", example = "별빛 그랜드마스터")
    val tierName: String? = null,

    @param:Schema(description = "프로필 이미지 UID", example = "f580e8e8-adee-4285-b181-3fed545e7be0")
    val assetUid: String?,

    @param:Schema(description = "친구 여부", example = "true")
    val isFriend: Boolean? = false,
) {
    companion object {
        fun from(user: UserEntity, isFriend: Boolean? = false, tierName: String? = null): UserSummary =
            UserSummary(
                id = user.id,
                uid = user.uid.toString(),
                username = user.username,
                nickname = user.nickname,
                address = user.address,
                detailAddress = user.detailAddress,
                phone = user.phone,
                mood = user.mood,
                moodEmoji = user.moodEmoji,
                mbti = user.mbti,
                expPoints = user.expPoints,
                tierName = tierName,
                assetUid = user.assetUid?.toString(),
                isFriend = isFriend
            )
    }
}
