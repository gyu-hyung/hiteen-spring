package kr.jiasoft.hiteen.feature.user.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import kr.jiasoft.hiteen.feature.school.domain.SchoolEntity
import kr.jiasoft.hiteen.feature.school.dto.SchoolDto
import kr.jiasoft.hiteen.feature.user.dto.CustomUserDetails
import kr.jiasoft.hiteen.feature.user.dto.UserResponse
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Table(name = "users")
data class UserEntity(
    /* 고유번호 */
    @Id
    @JsonIgnore
    val id: Long? = null,

    /* UUID, 외부 노출용 고유 식별자 */
    val uid: UUID = UUID.randomUUID(),

    /* 로그인 아이디 */
    val username: String,

    /* 이메일(고유) */
    val email: String? = null,

    /* 닉네임 */
    val nickname: String? = null,

    /* 비밀번호 해시 */
    @JsonIgnore
    val password: String,

    /* 권한(예: ADMIN/USER) */
    val role: String,

    /* 주소 */
    val address: String? = null,

    /* 상세 주소 */
    val detailAddress: String? = null,

    /* 전화번호 */
    val phone: String? = null,

    /* 사용자 상태/기분 코드 */
    val mood: String? = null,

    /* 티어 코드 문자열 */
    val tier: String? = null,

    /* 프로필 썸네일 Asset UID(assets.uid) */
    val assetUid: UUID? = null,

    /* 학교 id */
    val schoolId: Long? = null,

    /* 학년 */
    val grade: String? = null,

    /* 성별 */
    val gender: String? = null,

    /* 생년월일 */
    val birthday: LocalDate? = null,

    /* 생성자 사용자 ID(감사용) */
    val createdId: Long? = null,

    /* 생성 일시 */
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    /* 수정자 사용자 ID(감사용) */
    val updatedId: Long? = null,

    /* 수정 일시 */
    val updatedAt: OffsetDateTime? = null,

    /* 삭제자 사용자 ID(감사용) */
    val deletedId: Long? = null,

    /* 삭제 일시 */
    val deletedAt: OffsetDateTime? = null,
)

fun UserEntity.toUserDetails(): UserDetails = CustomUserDetails(this)

fun UserEntity.toResponse(): UserResponse = UserResponse(
    uid = this.uid.toString(),
    username = this.username,
    email = this.email,
    nickname = this.nickname,
    role = this.role,
    address = this.address,
    detailAddress = this.detailAddress,
    phone = this.phone,
    mood = this.mood,
    tier = this.tier,
    assetUid = this.assetUid?.toString(),
    grade = this.grade,
    gender = this.gender,
    birthday = this.birthday,
    createdAt = this.createdAt.toLocalDateTime(),
    updatedAt = this.updatedAt?.toLocalDateTime(),
    deletedAt = this.deletedAt?.toLocalDateTime(),
)

fun UserEntity.toResponseWithSchool(school: SchoolEntity? = null): UserResponse = UserResponse(
    uid = this.uid.toString(),
    username = this.username,
    email = this.email,
    nickname = this.nickname,
    role = this.role,
    address = this.address,
    detailAddress = this.detailAddress,
    phone = this.phone,
    mood = this.mood,
    tier = this.tier,
    assetUid = this.assetUid?.toString(),
    school = school?.let { SchoolDto(it.id!!, it.name) },
    grade = this.grade,
    gender = this.gender,
    birthday = this.birthday,
    createdAt = this.createdAt.toLocalDateTime(),
    updatedAt = this.updatedAt?.toLocalDateTime(),
    deletedAt = this.deletedAt?.toLocalDateTime(),
)