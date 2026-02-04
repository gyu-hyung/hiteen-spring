package kr.jiasoft.hiteen.feature.board.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.common.dto.ApiPageCursor
import kr.jiasoft.hiteen.feature.user.dto.UserSummary
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "게시글 응답 DTO")
@Table("boards")
data class BoardResponse(

    @Id
    @JsonIgnore
    @param:Schema(description = "게시글 PK (내부 관리용)", example = "1", hidden = true)
    val id: Long,

    @param:Schema(description = "게시글 UID", example = "550e8400-e29b-41d4-a716-446655440000")
    val uid: UUID,

    @param:Schema(description = "카테고리", example = "공지사항")
    val category: String,

    @param:Schema(description = "제목", example = "학교 행사 안내")
    val subject: String? = null,

    @param:Schema(description = "내용", example = "이번 주 금요일에 체육대회가 열립니다.")
    val content: String? = null,

    @param:Schema(description = "외부 링크", example = "https://example.com")
    val link: String? = null,

    @param:Schema(description = "조회수", example = "125")
    val hits: Int = 0,

    @param:Schema(description = "대표 첨부파일 UID", example = "550e8400-e29b-41d4-a716-446655441111")
    val assetUid: UUID? = null,

    @param:Schema(description = "첨부파일 UID 리스트", example = "[\"550e8400-e29b-41d4-a716-446655441111\"]")
    val attachments: List<UUID>? = null,

    @param:Schema(description = "(EVENT) 큰 배너 UID 리스트", example = "[\"550e8400-e29b-41d4-a716-446655441111\"]")
    val largeBanners: List<UUID>? = null,

    @param:Schema(description = "(EVENT) 작은 배너 UID 리스트", example = "[\"550e8400-e29b-41d4-a716-446655441112\"]")
    val smallBanners: List<UUID>? = null,

    @param:Schema(description = "시작일", example = "2025-09-01")
    val startDate: LocalDate? = null,

    @param:Schema(description = "종료일", example = "2025-09-30")
    val endDate: LocalDate? = null,

    @param:Schema(description = "상태", example = "ACTIVE")
    val status: String,

    @param:Schema(description = "주소", example = "서울특별시 강남구 테헤란로 123")
    val address: String?,

    @param:Schema(description = "상세 주소", example = "빌딩 5층")
    val detailAddress: String?,

    @param:Schema(description = "위도", example = "")
    val lat: Double? = null,

    @param:Schema(description = "경도", example = "")
    val lng: Double? = null,

    @field:Schema(description = "학년", example = "3")
    val grade: String? = null,

    @field:Schema(description = "학교 type", example = "3")
    val type: String? = null,

    @field:Schema(description = "학교 이름", example = "홍성여자중학교부설방송통신중학교")
    val schoolName: String? = null,

    @field:Schema(description = "잘린 학교 이름", example = "홍성여자중학교부설방송통신중학교")
    val modifiedSchoolName: String? = normalizeSchoolName(schoolName),

    @param:Schema(description = "작성 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val createdAt: OffsetDateTime,

    @param:Schema(description = "작성자 ID (내부용)", example = "1001", hidden = true)
    val createdId: Long,

    @param:Schema(description = "수정 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val updatedAt: OffsetDateTime? = null,

    @param:Schema(description = "삭제 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val deletedAt: OffsetDateTime? = null,

    @param:Schema(description = "좋아요 수", example = "42")
    val likeCount: Long = 0,

    @param:Schema(description = "댓글 수", example = "12")
    val commentCount: Long = 0,

    @param:Schema(description = "내가 좋아요 눌렀는지 여부", example = "true")
    val likedByMe: Boolean? = false,

    @param:Schema(description = "작성자 요약 정보")
    val user: UserSummary? = null,

    @param:Schema(description = "댓글 목록 (커서 기반 페이지네이션)")
    val comments: ApiPageCursor<BoardCommentResponse>? = null,
) {
    companion object {
        private fun normalizeSchoolName(raw: String?): String? {
            if (raw.isNullOrBlank()) return ""

            var name = raw

            // 1️⃣ 특수 케이스: 검정고시
            if (name.contains("검정고시")) {
                return "검정고시"
            }

            // 2️⃣ 행정용 불필요 키워드 제거
            val removeKeywords = listOf(
                "학력인정",
                "병설",
                "분교장",
                "부설",
                "캠퍼스",
                "교육센터",
                "공동실습소",
                "(2년제)",
                "테크노폴리스"
            )

            removeKeywords.forEach {
                name = name?.replace(it, "")
            }

            // 3️⃣ 공백 정리
            name = name?.replace("\\s+".toRegex(), "")

            // 4️⃣ 학교급 치환 (긴 것부터!)
            val replaceMap = listOf(
                "기계공업고등학교" to "기계공고",
                "공업고등학교" to "공고",
                "고등학교" to "고",
                "중학교" to "중",
                "초등학교" to "초"
            )

            replaceMap.forEach { (from, to) ->
                name = name?.replace(from, to)
            }

            return name?.trim()
        }

    }
}
