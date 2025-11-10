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


    @Query("""
        SELECT
            ROW_NUMBER() OVER (ORDER BY gs.score ASC, gs.created_at ASC) AS rank,
            u.id AS user_id,
            u.nickname,
            u.asset_uid,
            gs.score,
            gs.try_count,
            gs.created_at,
            gs.updated_at
        FROM game_scores gs
        JOIN season_participants sp ON gs.participant_id = sp.id
        JOIN users u ON sp.user_id = u.id
        JOIN seasons s ON gs.season_id = s.id
        WHERE gs.season_id = :seasonId
          AND gs.game_id   = :gameId
          AND sp.league    = :league
        ORDER BY gs.score ASC, gs.created_at ASC
        LIMIT :limit
    """)
    fun findSeasonRanking(seasonId: Long, gameId: Long, league: String, limit: Int = 100): Flow<RankingView>


    /**
     * 🔹 특정 유저의 실제 랭킹 1건 조회 (Top100 밖이라도)
     */
    @Query("""
        SELECT
            (SELECT COUNT(*) + 1
             FROM game_scores gs2
             JOIN season_participants sp2 ON gs2.participant_id = sp2.id
             WHERE gs2.season_id = gs.season_id
               AND gs2.game_id = gs.game_id
               AND sp2.league = sp.league
               AND (gs2.score < gs.score
                 OR (gs2.score = gs.score AND gs2.created_at < gs.created_at))
            ) AS rank,
            u.id AS user_id,
            u.nickname,
            u.asset_uid,
            gs.score,
            gs.try_count,
            gs.created_at,
            gs.updated_at
        FROM game_scores gs
        JOIN season_participants sp ON gs.participant_id = sp.id
        JOIN users u ON sp.user_id = u.id
        WHERE gs.season_id = :seasonId
          AND gs.game_id   = :gameId
          AND sp.league    = :league
          AND u.id         = :userId
        LIMIT 1
    """)
    suspend fun findMyRanking(
        seasonId: Long,
        gameId: Long,
        league: String,
        userId: Long
    ): RankingView?



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
