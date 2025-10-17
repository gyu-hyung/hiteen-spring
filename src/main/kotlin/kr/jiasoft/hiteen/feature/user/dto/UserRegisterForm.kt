package kr.jiasoft.hiteen.feature.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.validation.ValidPassword
import java.time.LocalDate
import java.util.UUID

@Schema(description = "사용자 회원가입 요청 DTO")
data class UserRegisterForm(

//    @field:NotBlank(message = "아이디는 필수입니다.")
//    @param:Schema(description = "사용자 계정명 (로그인 ID)", example = "chat1", maxLength = 50)
    var username: String? = null,

//    @field:NotBlank(message = "이메일은 필수입니다.")
//    @field:Email(message = "올바른 이메일 형식이어야 합니다.")
    @param:Schema(description = "이메일 주소", example = "user@example.com")
    val email: String? = null,

    @field:NotBlank(message = "닉네임은 필수입니다.")
    @field:Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
    @param:Schema(description = "닉네임", example = "홍길동", minLength = 2, maxLength = 20)
    val nickname: String,

    @field:NotBlank(message = "비밀번호는 필수입니다.")
    @field:ValidPassword
    @param:Schema(description = "비밀번호", example = "P@ssw0rd!")
    val password: String? = null,

    @param:Schema(description = "주소", example = "서울특별시 강남구")
    val address: String? = null,

    @param:Schema(description = "상세 주소", example = "101동 202호")
    val detailAddress: String? = null,

    @field:NotBlank(message = "연락처는 필수입니다.")
    @field:Pattern(
        regexp = "^01[0-9]{8,9}$",
        message = "올바른 휴대폰 번호 형식이어야 합니다. (예: 01012345678)"
    )
    @param:Schema(description = "휴대폰 번호", example = "01012345678")
    val phone: String,

    @param:Schema(description = "현재 기분", example = "기분좋음")
    val mood: String? = null,

    @param:Schema(description = "현재 기분 이모지", example = "E_001")
    val moodEmoji: String? = null,

    @param:Schema(description = "MBTI", example = "INTJ")
    val mbti: String? = null,

    @param:Schema(description = "등급/티어", example = "브론즈 1")
    val tier: String? = null,

    @param:Schema(description = "프로필 이미지 UID", example = "c264013d-bb1d-4d66-8d34-10962c022056")
    val assetUid: UUID? = null,

    @param:Schema(description = "학교 ID", example = "1")
    val schoolId: Long? = null,

    @param:Schema(description = "학년", example = "3")
    val grade: String? = null,

    @param:Schema(description = "성별", example = "M")
    val gender: String? = null,

    @param:Schema(description = "생일", example = "1999-12-01")
    val birthday: LocalDate? = null,

    @param:Schema(description = "초대코드", example = "TMUZTTCH6L")
    var inviteCode: String? = null,

//    @param:Schema(description = "초대 후 가입자수", example = "10")
//    val inviteJoins: Long = 0,

) {
    fun toEntity(encodedPassword: String, tierId: Long): UserEntity = UserEntity(
        username = username ?: "",
        email = email,
        nickname = nickname,
        password = encodedPassword,
        role = "USER",
        address = address,
        detailAddress = detailAddress,
        phone = phone,
        mood = mood,
        moodEmoji = moodEmoji,
        mbti = mbti,
        tierId = tierId,
        assetUid = assetUid,
        schoolId = schoolId,
        grade = grade,
        gender = gender,
        birthday = birthday,
        inviteCode = inviteCode,
//        inviteJoins = inviteJoins,
    )
}
