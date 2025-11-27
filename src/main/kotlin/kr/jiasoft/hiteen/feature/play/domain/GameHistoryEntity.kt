package kr.jiasoft.hiteen.feature.play.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("game_history")
data class GameHistoryEntity(

    @Id
    val id: Long = 0,

    val uid: UUID = UUID.randomUUID(),

    @Column("season_id")
    val seasonId: Long,

    @Column("participant_id")
    val participantId: Long,

    @Column("game_id")
    val gameId: Long,

    val score: Double? = null,

    val status: String, // PLAYING, DONE

    @Column("created_at")
    val createdAt: OffsetDateTime? = null,
)
