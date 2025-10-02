package kr.jiasoft.hiteen.feature.play.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.play.domain.GameRankingEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface GameRankingRepository : CoroutineCrudRepository<GameRankingEntity, Long> {
    fun findAllByGameId(gameId: Long): Flow<GameRankingEntity>
    fun findAllByParticipantId(participantId: Long): Flow<GameRankingEntity>

    @Query("""
        SELECT * 
        FROM game_rankings
        WHERE season_id = :seasonId
          AND game_id   = :gameId
        ORDER BY rank ASC
    """)
    fun findAllBySeasonIdAndGameId(seasonId: Long, gameId: Long): Flow<GameRankingEntity>

    @Query(
        """
        SELECT * 
        FROM game_rankings
        WHERE season_id = :seasonId
          AND game_id   = :gameId
          AND league    = :league
        ORDER BY rank ASC
        """
    )
    fun findAllBySeasonIdAndGameIdAndLeague(
        seasonId: Long,
        gameId: Long,
        league: String
    ): Flow<GameRankingEntity>

}
