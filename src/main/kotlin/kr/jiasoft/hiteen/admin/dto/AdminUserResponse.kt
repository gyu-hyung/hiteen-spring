package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.interest.dto.InterestUserResponse
import kr.jiasoft.hiteen.feature.level.domain.TierEntity
import kr.jiasoft.hiteen.feature.relationship.dto.RelationshipCounts
import kr.jiasoft.hiteen.feature.school.domain.SchoolClassesEntity
import kr.jiasoft.hiteen.feature.school.dto.SchoolDto
import kr.jiasoft.hiteen.feature.user.domain.UserPhotosEntity
import kr.jiasoft.hiteen.feature.user.dto.UserResponse
import java.time.LocalDate
import java.time.OffsetDateTime

data class AdminUserResponse(
    val id: Long,
    val uid: String,
    val assetUid: String? = null,
    val nickname: String,
    val username: String,
    val phone: String,
    val gender: String? = null,
    val birthday: LocalDate? = null,
    val schoolName: String? = null,
    val tierNameKr: String? = null,
    val tierAssetUid: String? = null,
    val locationMode: String,
    val point: Int = 0,
    val role: String,
    val level: Int,
    val mbti: String? = null,
    val mood: String? = null,

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val accessedAt: OffsetDateTime? = null,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val createdAt: OffsetDateTime,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val updatedAt: OffsetDateTime? = null,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val deletedAt: OffsetDateTime? = null,

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
            data: AdminUserResponse,
            user: UserResponse
        ): AdminUserResponse {
            return data.copy(
                school = user.school,
                schoolClass = user.schoolClass,
                tier = user.tier,
                relationshipCounts = user.relationshipCounts,
                interests = user.interests,
                photos = user.photos,
            )
        }
    }
}