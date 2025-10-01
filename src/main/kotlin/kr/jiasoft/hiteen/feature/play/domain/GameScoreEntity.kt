package kr.jiasoft.hiteen.feature.play.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("game_scores")
data class GameScoreEntity(
    @Id
    val id: Long = 0L,
    val seasonId: Long,
    val participantId: Long,
    val gameId: Long,
    val score: Long,
    val tryCount: Int = 1,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime? = null,
    val deletedAt: OffsetDateTime? = null
)