package kr.jiasoft.hiteen.feature.play.dto

import java.time.OffsetDateTime
import java.util.UUID

data class GameScoreWithParticipantView(
    val scoreId: Long,
    val participantId: Long,
    val userId: Long,
    val league: String,
    val score: Double,
    val tryCount: Int?,
    val createdAt: OffsetDateTime,
    val userNickname: String,
    val userAssetUid: UUID?
)
