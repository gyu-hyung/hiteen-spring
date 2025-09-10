package kr.jiasoft.hiteen.feature.user.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import kr.jiasoft.hiteen.feature.school.domain.SchoolEntity
import kr.jiasoft.hiteen.feature.school.dto.SchoolDto
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Table("users")
data class UserResponse(
    @Id
    @JsonIgnore
    val id: Long?,
    val uid: String,
    val username: String,
    val email: String?,
    val nickname: String?,
    val role: String,
    val address: String?,
    val detailAddress: String?,
    val phone: String?,
    val mood: String?,
    val tier: String?,
    val assetUid: String?,
    val school: SchoolDto?,
    val grade: String?,
    val gender: String?,
    val birthday: LocalDate?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?,
    val deletedAt: LocalDateTime?
) {
    companion object {
        fun from(entity: UserEntity, school: SchoolEntity? = null): UserResponse =
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
                tier = entity.tier,
                assetUid = entity.assetUid?.toString(),
                school = school?.let { SchoolDto(it.id!!, it.name) },
                grade = entity.grade,
                gender = entity.gender,
                birthday = entity.birthday,
                createdAt = entity.createdAt.toLocalDateTime(),
                updatedAt = entity.updatedAt?.toLocalDateTime(),
                deletedAt = entity.deletedAt?.toLocalDateTime()
            )
    }
}
