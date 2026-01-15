package kr.jiasoft.hiteen.feature.play.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.play.domain.GameScoreEntity
import kr.jiasoft.hiteen.feature.play.dto.FriendRankView
import kr.jiasoft.hiteen.feature.play.dto.GameScoreWithParticipantView
import kr.jiasoft.hiteen.feature.play.dto.RankingView
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface GameScoreRepository : CoroutineCrudRepository<GameScoreEntity, Long> {

    suspend fun findBySeasonIdAndParticipantIdAndGameId(seasonId: Long, participantId: Long, gameId: Long): GameScoreEntity?


    @Query("""
        SELECT
            gs.id,
            ROW_NUMBER() OVER (ORDER BY gs.score ASC, CASE WHEN gs.updated_at IS NOT NULL THEN gs.updated_at ELSE gs.created_at END ASC) AS rank,
            u.id AS user_id,
            u.nickname,
            u.grade,
            (SELECT "type" FROM schools WHERE id = u.school_id LIMIT 1) AS type,
            (SELECT "name" FROM schools WHERE id = u.school_id LIMIT 1) AS school_name,
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
          AND (
                :participantIds IS NULL 
                OR sp.id = ANY(:participantIds)
              )
        ORDER BY gs.score ASC, CASE WHEN gs.updated_at IS NOT NULL THEN gs.updated_at ELSE gs.created_at END ASC
        LIMIT :limit
    """)
    fun findSeasonRankingFiltered(
        seasonId: Long,
        gameId: Long,
        league: String,
        participantIds: Array<Long>?,
        limit: Int = 100
    ): Flow<RankingView>


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
        SELECT 
            gs.id             AS score_id,
            sp.id             AS participant_id,
            u.id              AS user_id,
            sp.league         AS league,
            gs.score          AS score,
            gs.try_count      AS try_count,
            gs.created_at     AS created_at,
            gs.updated_at     AS updated_at,
            u.nickname        AS user_nickname,
            u.asset_uid       AS user_asset_uid
        FROM game_scores gs
        JOIN season_participants sp ON sp.id = gs.participant_id
        JOIN users u ON u.id = sp.user_id
        WHERE gs.season_id = :seasonId
          AND gs.game_id   = :gameId
        ORDER BY sp.league ASC, gs.score ASC, CASE WHEN gs.updated_at IS NOT NULL THEN gs.updated_at ELSE gs.created_at END ASC
    """)
    fun findScoresWithParticipantsBySeasonAndGame(
        seasonId: Long,
        gameId: Long
    ): Flow<GameScoreWithParticipantView>


    /**
     * 친구 랭킹(친구+나) 추월 판정용 최소 정보(rank, userId)
     */
    @Query("""
        SELECT
            ROW_NUMBER() OVER (ORDER BY gs.score ASC, CASE WHEN gs.updated_at IS NOT NULL THEN gs.updated_at ELSE gs.created_at END ASC) AS rank,
            u.id AS user_id
        FROM game_scores gs
        JOIN season_participants sp ON gs.participant_id = sp.id
        JOIN users u ON sp.user_id = u.id
        WHERE gs.season_id = :seasonId
          AND gs.game_id   = :gameId
          AND sp.league    = :league
          AND (
                :participantIds IS NULL
                OR sp.id = ANY(:participantIds)
              )
        ORDER BY gs.score ASC, CASE WHEN gs.updated_at IS NOT NULL THEN gs.updated_at ELSE gs.created_at END ASC
        LIMIT :limit
    """)
    fun findFriendRanks(
        seasonId: Long,
        gameId: Long,
        league: String,
        participantIds: Array<Long>?,
        limit: Int = 200,
    ): Flow<FriendRankView>

}
