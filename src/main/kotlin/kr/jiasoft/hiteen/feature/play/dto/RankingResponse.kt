package kr.jiasoft.hiteen.feature.play.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "랭킹 응답 DTO")
data class RankingResponse(

    @field:Schema(description = "순위", example = "1")
    val rank: Int,

    @JsonIgnore
    val userId: Long,

    @field:Schema(description = "닉네임", example = "닉네임")
    val nickname: String,

    @field:Schema(description = "학년", example = "3")
    val grade: String? = null,

    @field:Schema(description = "학교 type", example = "3")
    val type: String? = null,

    @field:Schema(description = "학교 이름", example = "홍성여자중학교부설방송통신중학교")
    val schoolName: String? = null,

    @field:Schema(description = "잘린 학교 이름", example = "홍성여자중학교부설방송통신중학교")
    val modifiedSchoolName: String? = normalizeSchoolName(schoolName),

    @field:Schema(description = "회원 프로필 UUID", example = "UUID")
    val profileImageUrl: String?,

    @field:Schema(description = "점수(raw)", example = "1013")
    val score: Double,

    @field:Schema(description = "표출 시간 (포맷된)", example = "00:10:13")
    val displayTime: String = formatScore(score),

    @field:Schema(description = "시도 횟수", example = "1")
    val tryCount: Long? = null,

    @field:Schema(description = "랭킹 생성 시간", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val createdAt: OffsetDateTime? = null,

    @field:Schema(description = "랭킹 수정 시간", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val updatedAt: OffsetDateTime? = null,

    @field:Schema(description = "나의 랭킹인지 여부", example = "true")
    val isMe: Boolean = false
) {
    companion object {
        private fun formatScore(score: Double): String {
            val totalMillis = (score * 1000).toLong()
            val minutes = (totalMillis / 60000) % 100
            val seconds = (totalMillis / 1000) % 60
            val millis = (totalMillis / 10) % 100
            return String.format("%02d:%02d:%02d", minutes, seconds, millis)
        }

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
