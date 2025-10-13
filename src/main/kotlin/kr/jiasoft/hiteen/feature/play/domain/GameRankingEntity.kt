package kr.jiasoft.hiteen.feature.play.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("game_rankings")
data class GameRankingEntity(
    @Id
    val id: Long? = null,

    val seasonId: Long,
    val league: String,
    val gameId: Long,
    val rank: Int,
    val score: Long,

    val participantId: Long,
    val userId: Long,

    val nickname: String,
    val profileImage: String? = null,

    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
