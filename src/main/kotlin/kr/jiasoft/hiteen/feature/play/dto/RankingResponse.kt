package kr.jiasoft.hiteen.feature.play.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema

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
    @field:Schema(description = "점수", example = "1013")
    val score: Long,
    @field:Schema(description = "표출 시간", example = "00:10:13")
    val displayTime: String,
    @field:Schema(description = "시도 횟수", example = "1")
    val tryCount: Long,
    @field:Schema(description = "나의 랭킹인지 여부", example = "true")
    val isMe: Boolean = false
)