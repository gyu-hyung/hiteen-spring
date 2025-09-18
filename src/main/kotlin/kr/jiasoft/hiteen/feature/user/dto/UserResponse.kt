package kr.jiasoft.hiteen.feature.user.dto

import com.fasterxml.jackson.annotation.JsonIgnore
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

@Table("users")
data class UserResponse(
    @Id
    @JsonIgnore
    val id: Long,
    val uid: String,
    val username: String,
    val email: String?,
    val nickname: String,
    val role: String,
    val address: String?,
    val detailAddress: String?,
    val phone: String,
    val mood: String?,
    val mbti: String?,
    val tier: String?,
    val assetUid: String?,
    val grade: String?,
    val gender: String?,
    val birthday: LocalDate?,
    val isFollowed: Boolean = false,
    val isFriend: Boolean = false,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?,
    val deletedAt: LocalDateTime?,
    val school: SchoolDto?,
    val relationshipCounts: RelationshipCounts?,
    val interests: List<InterestUserResponse>?,
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
                    school = school?.let { SchoolDto(it.id!!, it.name) },
                    relationshipCounts = relationshipCounts,
                    interests = interests,
                    photos = photos,
                )
    }
}
