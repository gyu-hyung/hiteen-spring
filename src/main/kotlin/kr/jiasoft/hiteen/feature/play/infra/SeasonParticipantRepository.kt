package kr.jiasoft.hiteen.feature.play.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.play.domain.SeasonParticipantEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

// ========================
// 게임 회차 참가자
// ========================
@Repository
interface SeasonParticipantRepository : CoroutineCrudRepository<SeasonParticipantEntity, Long> {

    @Query(
        """
        SELECT sp.*
        FROM season_participants sp
        JOIN seasons s ON sp.season_id = s.id
        WHERE sp.user_id = :userId
          AND s.id = :seasonId
          AND s.status = 'ACTIVE'
        ORDER BY s.start_date DESC, s.id DESC
        LIMIT 1
        """
    )
    suspend fun findActiveParticipant(userId: Long, seasonId: Long): SeasonParticipantEntity?


    @Query("""
       SELECT sp.*
       FROM season_participants sp
       JOIN game_scores gs ON gs.season_id = sp.season_id AND sp.id = gs.participant_id
       WHERE sp.season_id = :seasonId
         AND gs.game_id = :gameId
         AND sp.league = :league
         AND sp.user_id IN (:userIds)
    """)
    fun findByUserIds(
        seasonId: Long,
        gameId: Long,
        league: String,
        userIds: Set<Long>
    ): Flow<SeasonParticipantEntity>


}