package kr.jiasoft.hiteen.feature.user.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size
import kr.jiasoft.hiteen.validation.ValidPassword
import java.time.LocalDate
import java.util.UUID

@Schema(description = "사용자 정보 수정 요청 DTO")
data class UserUpdateForm(

//    val username: String? = null,

    @param:Schema(description = "이메일", example = "hong@test.com", maxLength = 255)
    @field:Email(message = "올바른 이메일 형식이어야 합니다.")
    val email: String? = null,

    @param:Schema(description = "닉네임", example = "홍길동", maxLength = 50)
    @field:Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
    val nickname: String? = null,

    // 값이 존재하지 않을 시 기존 데이터 유지
    @field:ValidPassword
    val password: String? = null,

    @param:Schema(description = "주소", example = "서울특별시 강남구 테헤란로 123", maxLength = 255)
    val address: String? = null,

    @param:Schema(description = "상세 주소", example = "101동 202호", maxLength = 255)
    val detailAddress: String? = null,

//    @param:Schema(description = "휴대폰 번호", example = "01012345678", maxLength = 30)
//    val phone: String? = null,

    @param:Schema(description = "기분 상태", example = "기분좋음", maxLength = 30)
    val mood: String? = null,

    @param:Schema(description = "기분 상태 이모지", example = "E_001", maxLength = 30)
    val moodEmoji: String? = null,

    @param:Schema(description = "MBTI", example = "INTJ", maxLength = 30)
    val mbti: String? = null,

//    @param:Schema(description = "티어", example = "브론즈 1", maxLength = 30)
//    val tier: String? = null,

    val assetUid: UUID? = null,

    @param:Schema(description = "학교 ID (schools.id)", example = "1")
    val schoolId: Long? = null,

    @param:Schema(description = "학년", example = "2학년", maxLength = 30)
    val grade: String? = null,

    @param:Schema(description = "성별", example = "M", allowableValues = ["M", "F"], maxLength = 30)
    val gender: String? = null,

    @param:Schema(description = "생년월일", example = "1999-12-01")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val birthday: LocalDate? = null,

    @param:Schema(description = "프로필 데코레이션 코드", example = "P_001")
    val profileDecorationCode: String? = null,
)
