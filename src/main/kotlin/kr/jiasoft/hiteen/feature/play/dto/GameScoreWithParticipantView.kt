package kr.jiasoft.hiteen.feature.play.dto

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class GameScoreWithParticipantView(
    val scoreId: Long,
    val participantId: Long,
    val userId: Long,
    val league: String,
    val score: BigDecimal,
    val tryCount: Int?,
    val createdAt: OffsetDateTime,
    val userNickname: String,
    val userAssetUid: UUID?
)
