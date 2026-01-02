package kr.jiasoft.hiteen.feature.play.dto

import kr.jiasoft.hiteen.feature.play.domain.GameScoreEntity
import java.math.BigDecimal
import java.time.OffsetDateTime

data class GameScoreResponse (
    val id: Long = 0L,
    val seasonId: Long,
    val participantId: Long,
    val gameId: Long,
    val score: BigDecimal,
    val tryCount: Int = 0,
    val totalTryCount: Int = 0,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime? = null,
    val deletedAt: OffsetDateTime? = null,
    val nowScore: BigDecimal,
) {
    companion object {
        fun fromEntity(entity: GameScoreEntity, nowScore: BigDecimal, advantage: BigDecimal = BigDecimal.ZERO) = GameScoreResponse(
            id = entity.id,
            seasonId = entity.seasonId,
            participantId = entity.participantId,
            gameId = entity.gameId,
            score = entity.score,
            tryCount = entity.tryCount,
            totalTryCount = entity.totalTryCount,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            deletedAt = entity.deletedAt,
            nowScore = nowScore
                .subtract(advantage)
//                .setScale(2, RoundingMode.DOWN),
            )
    }
}