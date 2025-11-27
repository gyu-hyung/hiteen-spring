package kr.jiasoft.hiteen.feature.play.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "게임 시작 요청 DTO")
data class GameStartRequest(
    @param:Schema(description = "게임 ID", example = "1")
    val gameId: Long,

    @param:Schema(description = "광고 리워드 트랜잭션 ID", example = "1")
    val transactionId: String? = null,

    @param:Schema(description = "재도전 타입", example = "POINT / AD")
    val retryType: String? = null
)