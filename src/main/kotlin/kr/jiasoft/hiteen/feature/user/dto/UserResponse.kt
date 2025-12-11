package kr.jiasoft.hiteen.feature.user.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.interest.dto.InterestUserResponse
import kr.jiasoft.hiteen.feature.level.domain.TierEntity
import kr.jiasoft.hiteen.feature.relationship.dto.RelationshipCounts
import kr.jiasoft.hiteen.feature.school.domain.SchoolClassesEntity
import kr.jiasoft.hiteen.feature.school.domain.SchoolEntity
import kr.jiasoft.hiteen.feature.school.dto.SchoolDto
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.domain.UserPhotosEntity
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "사용자 응답 DTO")
@Table("users")
data class UserResponse(

    @Id
    @JsonIgnore
    @param:Schema(description = "사용자 내부 식별자 (DB PK)", example = "1", hidden = true)
    val id: Long,

    @param:Schema(description = "사용자 UID", example = "c264013d-bb1d-4d66-8d34-10962c022056")
    val uid: String,

    @param:Schema(description = "사용자 계정명 (로그인 ID)", example = "chat1")
    val username: String,

    @param:Schema(description = "이메일 주소", example = "user@example.com")
    val email: String?,

    @param:Schema(description = "닉네임", example = "홍길동", maxLength = 50)
    val nickname: String,

    @param:Schema(description = "역할/권한", example = "USER")
    val role: String,

    @param:Schema(description = "주소", example = "서울특별시 강남구")
    val address: String?,

    @param:Schema(description = "상세 주소", example = "101동 202호")
    val detailAddress: String?,

    @param:Schema(description = "전화번호", example = "01012345678")
    val phone: String,

    @param:Schema(description = "현재 기분", example = "기분좋음")
    val mood: String?,

    @param:Schema(description = "현재 기분 이모지", example = "E_001")
    val moodEmoji: String?,

    @param:Schema(description = "MBTI", example = "INTJ")
    val mbti: String?,

    @param:Schema(description = "현재 누적 경험치",example = "9999")
    val expPoint: Long,

    @param:Schema(description = "프로필 이미지 UID", example = "a0e9f441-3669-4cfb-aac5-1c38533d87c1")
    val assetUid: String?,

    @param:Schema(description = "학년", example = "3")
    val grade: String?,

    @param:Schema(description = "성별", example = "M")
    val gender: String?,

    @param:Schema(description = "생일", example = "1999-12-01")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val birthday: LocalDate?,

    @param:Schema(description = "프로필 데코레이션 코드", example = "P_001")
    val profileDecorationCode: String? = null,

    @param:Schema(description = "초대코드", example = "TMUZTTCH6L")
    val inviteCode: String? = null,

    @param:Schema(description = "초대 후 가입자수", example = "10")
    val inviteJoins: Long? = null,

    @param:Schema(description = "위치 모드 활성화 여부", example = "true")
    val locationMode: Boolean,

    @param:Schema(description = "팔로우 여부", example = "false")
    val isFollowed: Boolean = false,

    @param:Schema(description = "친구 여부", example = "true")
    val isFriend: Boolean = false,

    @param:Schema(description = "친구 요청 여부", example = "true")
    val isFriendRequested: Boolean = false,

    @param:Schema(description = "팔로우 요청 여부", example = "false")
    val isFollowedRequested: Boolean = false,

    @param:Schema(description = "생성 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val createdAt: LocalDateTime,

    @param:Schema(description = "수정 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val updatedAt: LocalDateTime?,

    @param:Schema(description = "삭제 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val deletedAt: LocalDateTime?,

    @param:Schema(description = "학교 정보")
    val school: SchoolDto?,

    @param:Schema(description = "학교 정보")
    val schoolClass: SchoolClassesEntity?,

    @param:Schema(description = "티어 정보")
    val tier: TierEntity?,

    @param:Schema(description = "관계 카운트 정보 (친구/팔로워 등)")
    val relationshipCounts: RelationshipCounts?,

    @param:Schema(description = "사용자 관심사 목록")
    val interests: List<InterestUserResponse>?,

    @param:Schema(description = "사용자 사진 목록")
    val photos: List<UserPhotosEntity>?,
) {
    companion object {
        fun from(
                entity: UserEntity,
                school: SchoolEntity? = null,
                classes: SchoolClassesEntity? = null,
                tier: TierEntity? = null,
                interests: List<InterestUserResponse>? = null,
                relationshipCounts: RelationshipCounts? = null,
                photos: List<UserPhotosEntity>? = null,
                isFriend: Boolean = false,
                isFollowed: Boolean = false,
                isFriendRequested: Boolean = false,
                isFollowedRequested: Boolean = false,
            ): UserResponse =
                UserResponse(
                    id = entity.id,
                    uid = entity.uid.toString(),
                    username = entity.username,
                    email = entity.email,
                    nickname = entity.nickname,
                    role = entity.role,
                    address = entity.address,
                    detailAddress = entity.detailAddress,
                    phone = entity.phone,
                    mood = entity.mood,
                    moodEmoji = entity.moodEmoji,
                    mbti = entity.mbti,
                    expPoint = entity.expPoints,
                    assetUid = entity.assetUid?.toString(),
                    grade = entity.grade,
                    gender = entity.gender,
                    birthday = entity.birthday,
                    profileDecorationCode = entity.profileDecorationCode,
                    inviteCode = entity.inviteCode,
                    inviteJoins = entity.inviteJoins,
                    isFriend = isFriend,
                    isFollowed = isFollowed,
                    isFriendRequested = isFriendRequested,
                    isFollowedRequested = isFollowedRequested,
                    locationMode = entity.locationMode,
                    createdAt = entity.createdAt.toLocalDateTime(),
                    updatedAt = entity.updatedAt?.toLocalDateTime(),
                    deletedAt = entity.deletedAt?.toLocalDateTime(),
                    school = school?.let { SchoolDto.from(it) },
                    schoolClass = classes,
                    tier = tier,
                    relationshipCounts = relationshipCounts,
                    interests = interests,
                    photos = photos,
        )

        fun empty() = UserResponse(
            id = 0L,
            uid = "",
            username = "",
            email = null,
            nickname = "",
            role = "USER",
            address = null,
            detailAddress = null,
            phone = "",
            mood = null,
            moodEmoji = null,
            mbti = null,
            expPoint = 0L,
            assetUid = null,
            grade = null,
            gender = null,
            birthday = null,
            profileDecorationCode = null,
            inviteCode = null,
            inviteJoins = 0L,
            isFollowed = false,
            isFriend = false,
            isFriendRequested = false,
            isFollowedRequested = false,
            locationMode = false,
            createdAt = LocalDateTime.now(),
            updatedAt = null,
            deletedAt = null,
            school = null,
            schoolClass = null,
            tier = null,
            relationshipCounts = null,
            interests = emptyList(),
            photos = emptyList()
        )

    }
}
