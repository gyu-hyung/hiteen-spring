package kr.jiasoft.hiteen.feature.play.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.play.domain.GameScoreEntity
import kr.jiasoft.hiteen.feature.play.dto.RankingView
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface GameScoreRepository : CoroutineCrudRepository<GameScoreEntity, Long> {
    suspend fun findByParticipantIdAndGameId(participantId: Long, gameId: Long): GameScoreEntity?
    suspend fun findBySeasonIdAndParticipantIdAndGameId(seasonId: Long, participantId: Long, gameId: Long): GameScoreEntity?
    fun findAllByParticipantId(participantId: Long): Flow<GameScoreEntity>
    fun findAllByGameId(gameId: Long): Flow<GameScoreEntity>
    fun findBySeasonIdAndGameId(seasonId: Long, gameId: Long): Flow<GameScoreEntity>

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
        ) ranked
        ORDER BY ranked.rank ASC
        """
    )
    fun findSeasonRanking(seasonId: Long, gameId: Long): Flow<RankingView>


    //
    @Query("""
        SELECT * 
        FROM game_scores 
        WHERE season_id = :seasonId AND game_id = :gameId
        ORDER BY score ASC, created_at ASC
    """)
    fun findBySeasonIdAndGameIdOrderByScoreAsc(seasonId: Long, gameId: Long): Flow<GameScoreEntity>


}
