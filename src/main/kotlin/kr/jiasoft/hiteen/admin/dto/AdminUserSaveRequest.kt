package kr.jiasoft.hiteen.admin.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.util.UUID

@Schema(description = "관리자용 사용자 등록/수정 요청")
data class AdminUserSaveRequest(
    @param:Schema(description = "사용자 ID. null이면 신규 생성", example = "1")
    val id: Long? = null,

    @param:Schema(description = "로그인 아이디(username)")
    val username: String? = null,

    @param:Schema(description = "이메일")
    val email: String? = null,

    @param:Schema(description = "닉네임")
    val nickname: String? = null,

    @param:Schema(description = "전화번호")
    val phone: String? = null,

    @param:Schema(description = "권한(예: USER/ADMIN). null이면 변경 안 함")
    val role: String? = null,

    @param:Schema(description = "성별")
    val gender: String? = null,

    @param:Schema(description = "생년월일")
    val birthday: LocalDate? = null,

    @param:Schema(description = "주소")
    val address: String? = null,

    @param:Schema(description = "상세주소")
    val detailAddress: String? = null,

    @param:Schema(description = "기분 코드")
    val mood: String? = null,

    @param:Schema(description = "기분 이모지 코드")
    val moodEmoji: String? = null,

    @param:Schema(description = "MBTI")
    val mbti: String? = null,

    @param:Schema(description = "프로필 썸네일 asset uid")
    val assetUid: UUID? = null,

    @param:Schema(description = "위치 공개 모드")
    val locationMode: Boolean? = null,

    @param:Schema(description = "학교 ID")
    val schoolId: Long? = null,

    @param:Schema(description = "학급 ID")
    val classId: Long? = null,

    @param:Schema(description = "티어 ID")
    val tierId: Long? = null,

    @param:Schema(description = "학년도")
    val year: Int? = null,

    @param:Schema(description = "사용자 관심사 ID 목록. null이면 변경 안 함, 빈 배열이면 전체 삭제")
    val interestIds: List<Long>? = null,

    @param:Schema(description = "사용자 사진 asset uid 목록. null이면 변경 안 함, 빈 배열이면 전체 삭제")
    val photoUids: List<UUID>? = null,
)

