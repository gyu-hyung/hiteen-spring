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

    @field:Schema(description = "회원 프로필 UUID", example = "UUID")
    val profileImageUrl: String?,

    @field:Schema(description = "점수(raw)", example = "1013")
    val score: Double,

    @field:Schema(description = "표출 시간 (포맷된)", example = "00:10:13")
    val displayTime: String = formatScore(score),

    @field:Schema(description = "시도 횟수", example = "1")
    val tryCount: Long,

    @field:Schema(description = "랭킹 생성 시간", example = "2025.09.18 10:15")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val createdAt: OffsetDateTime? = null,

    @field:Schema(description = "랭킹 수정 시간", example = "2025.09.18 10:15")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val updatedAt: OffsetDateTime? = null,

    @field:Schema(description = "나의 랭킹인지 여부", example = "true")
    val isMe: Boolean = false
) {
    companion object {
        /**
         * Double 점수를 "MM:SS:MS" 형태로 포맷
         * 예: 1013.08 → "00:10:13"
         */
        private fun formatScore(score: Double): String {
            val raw = score.toLong() // Double → Long 변환
            val minutes = (raw / 10000).toInt()
            val seconds = ((raw / 100) % 100).toInt()
            val millis = (raw % 100).toInt()
            return String.format("%02d:%02d:%02d", minutes, seconds, millis)
        }
    }

}
