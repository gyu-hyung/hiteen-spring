package kr.jiasoft.hiteen.feature.play.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "랭킹 정보 view DTO")
data class RankingView(
    @field:Schema(description = "랭킹 순위", example = "1")
    val rank: Int,

    @field:Schema(description = "유저 ID", example = "1")
    val userId: Long,

    @field:Schema(description = "닉네임", example = "nickname")
    val nickname: String,

    @field:Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
    val assetUid: String?,

    @field:Schema(description = "점수", example = "63.12 -> 01:03:12")
    val score: Double,

    @field:Schema(description = "시도 횟수", example = "2")
    val tryCount: Long,

    @param:Schema(description = "랭킹 생성 시간", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val createdAt: OffsetDateTime? = null,

    @param:Schema(description = "랭킹 생성 시간", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val updatedAt: OffsetDateTime? = null,
)
