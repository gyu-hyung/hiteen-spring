package kr.jiasoft.hiteen.feature.user.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.interest.dto.InterestUserResponse
import kr.jiasoft.hiteen.feature.relationship.dto.RelationshipCounts
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

    @param:Schema(description = "MBTI", example = "INTJ")
    val mbti: String?,

    @param:Schema(description = "등급/티어", example = "브론즈 1")
    val tier: String?,

    @param:Schema(description = "프로필 이미지 UID", example = "a0e9f441-3669-4cfb-aac5-1c38533d87c1")
    val assetUid: String?,

    @param:Schema(description = "학년", example = "3")
    val grade: String?,

    @param:Schema(description = "성별", example = "M")
    val gender: String?,

    @param:Schema(description = "생일", example = "1999-12-01")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val birthday: LocalDate?,

    @param:Schema(description = "팔로우 여부", example = "false")
    val isFollowed: Boolean = false,

    @param:Schema(description = "친구 여부", example = "true")
    val isFriend: Boolean = false,

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
                interests: List<InterestUserResponse>? = null,
                relationshipCounts: RelationshipCounts? = null,
                photos: List<UserPhotosEntity>? = null,
                isFollowed: Boolean = false,
                isFriend: Boolean = false,
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
                    mbti = entity.mbti,
                    tier = entity.tier,
                    assetUid = entity.assetUid?.toString(),
                    grade = entity.grade,
                    gender = entity.gender,
                    birthday = entity.birthday,
                    isFollowed = isFollowed,
                    isFriend = isFriend,
                    createdAt = entity.createdAt.toLocalDateTime(),
                    updatedAt = entity.updatedAt?.toLocalDateTime(),
                    deletedAt = entity.deletedAt?.toLocalDateTime(),
                    school = school?.let { SchoolDto.from(it) },
                    relationshipCounts = relationshipCounts,
                    interests = interests,
                    photos = photos,
                )
    }
}
