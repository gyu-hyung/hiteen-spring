package kr.jiasoft.hiteen.feature.play.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("season_participants")
data class SeasonParticipantEntity(
    @Id
    val id: Long = 0L,
    val seasonId: Long,
    val userId: Long,
    val league: String,
    val tierId: Long,
    val joinedAt: OffsetDateTime = OffsetDateTime.now(),
    val joinedType: String = "INITIAL"
)