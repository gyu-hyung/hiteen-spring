package kr.jiasoft.hiteen.feature.play.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "점수 등록 요청 DTO")
data class ScoreRequest(
    @param:Schema(description = "게임 ID", example = "1")
    val gameId: Long,
    @param:Schema(description = "점수", example = "1")
    val score: Long,
)