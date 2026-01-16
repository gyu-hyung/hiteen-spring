package kr.jiasoft.hiteen.feature.play.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("reward_league_start_notifications")
data class RewardLeagueStartNotificationEntity(
    @Id
    val id: Long = 0L,

    val seasonId: Long,
    val league: String,
    val gameId: Long,

    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)

