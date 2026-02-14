package kr.jiasoft.hiteen.feature.play.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.common.helpers.SchoolNameHelper.normalizeSchoolName
import java.math.BigDecimal
import java.math.RoundingMode
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
    val score: BigDecimal,

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
        private fun formatScore(score: BigDecimal): String {
            // 초 → 밀리초 (버림)
            val totalMillis = score
                .multiply(BigDecimal("1000"))
                .setScale(0, RoundingMode.DOWN)
                .toLong()

            val minutes = totalMillis / 60_000
            val seconds = (totalMillis % 60_000) / 1_000
            val centiseconds = (totalMillis % 1_000) / 10   // 1/100초

            return String.format("%02d:%02d:%02d", minutes, seconds, centiseconds)
        }

    }
}
