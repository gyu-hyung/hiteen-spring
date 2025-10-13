package kr.jiasoft.hiteen.feature.play.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.play.domain.GameScoreEntity
import kr.jiasoft.hiteen.feature.play.dto.RankingView
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface GameScoreRepository : CoroutineCrudRepository<GameScoreEntity, Long> {

    suspend fun findBySeasonIdAndParticipantIdAndGameId(seasonId: Long, participantId: Long, gameId: Long): GameScoreEntity?


    // 실시간 랭킹
    @Query(
        """
        SELECT ranked.rank,
               ranked.user_id,
               ranked.nickname,
               ranked.asset_uid,
               ranked.score,
               ranked.try_count,
               ranked.created_at,
               ranked.updated_at          
        FROM (
            SELECT u.id AS user_id,
                   u.nickname,
                   u.asset_uid,
                   gs.score,
                   gs.try_count,
                   gs.created_at,
                   gs.updated_at,
                   ROW_NUMBER() OVER (ORDER BY gs.score ASC, gs.created_at ASC) AS rank
            FROM game_scores gs
            JOIN season_participants sp ON gs.participant_id = sp.id
            JOIN users u ON sp.user_id = u.id
            JOIN seasons s ON gs.season_id = s.id
            WHERE gs.season_id = :seasonId
              AND gs.game_id   = :gameId
              AND sp.league = :league
        ) ranked
        ORDER BY ranked.rank ASC
        """
    )
    fun findSeasonRanking(seasonId: Long, gameId: Long, league: String): Flow<RankingView>



    @Query("""
        SELECT gs.*
        FROM game_scores gs
        JOIN season_participants sp ON sp.id = gs.participant_id
        WHERE gs.season_id = :seasonId
          AND gs.game_id = :gameId
          AND sp.season_id = :seasonId
        ORDER BY sp.league ASC, gs.score ASC, gs.created_at ASC
    """)
    fun findScoresWithParticipantsBySeasonAndGame(
        seasonId: Long,
        gameId: Long
    ): Flow<GameScoreEntity>




}
