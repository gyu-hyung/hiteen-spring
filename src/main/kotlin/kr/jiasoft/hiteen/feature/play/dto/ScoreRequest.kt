package kr.jiasoft.hiteen.feature.play.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin

@Schema(description = "점수 등록 요청 DTO")
data class ScoreRequest(
    @param:Schema(description = "게임 ID", example = "1")
    val gameId: Long,

    @param:Schema(description = "점수", example = "1")
    @field:DecimalMin("0.0", message = "점수는 0 이상이어야 합니다.")
    @field:DecimalMax("5999.99", message = "점수는 99:99:99 (999999.99) 이하이어야 합니다.")
    val score: Double,

    @param:Schema(description = "광고 리워드 트랜잭션 ID", example = "1")
    val transactionId: String? = null,

    @param:Schema(description = "재도전 타입", example = "POINT / AD")
    val retryType: String? = null
)