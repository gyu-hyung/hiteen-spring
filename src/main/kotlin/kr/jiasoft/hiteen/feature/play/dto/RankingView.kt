package kr.jiasoft.hiteen.feature.play.dto

import io.swagger.v3.oas.annotations.media.Schema

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

    @field:Schema(description = "점수", example = "100")
    val score: Long,

    @field:Schema(description = "시도 횟수", example = "2")
    val tryCount: Long
)
