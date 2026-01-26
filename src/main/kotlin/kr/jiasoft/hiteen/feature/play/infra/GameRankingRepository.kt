package kr.jiasoft.hiteen.feature.play.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.play.domain.GameRankingEntity
import kr.jiasoft.hiteen.feature.play.dto.RankingRow
import kr.jiasoft.hiteen.feature.play.dto.RankingView
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

//    @Query(
//        """
//        SELECT *
//        FROM game_rankings
//        WHERE season_id = :seasonId
//          AND game_id   = :gameId
//          AND league    = :league
//        ORDER BY rank ASC
//        """
//    )
    @Query("""
        SELECT
            gr.id, 
            season_id, 
            league, 
            game_id, 
            rank, 
            score, 
            participant_id, 
            user_id,
            u.nickname,
            u.grade,
            (SELECT "type" FROM schools WHERE id = u.school_id LIMIT 1) AS type,
            (SELECT "name" FROM schools WHERE id = u.school_id LIMIT 1) AS school_name,
            profile_image as asset_uid,
            gr.created_at
        FROM game_rankings gr
        LEFT JOIN users u ON gr.user_id = u.id
        WHERE season_id = :seasonId
          AND game_id   = :gameId
          AND league    = :league
          AND COALESCE(u.role, '') <> 'ADMIN'
        ORDER BY rank ASC
    """)
    fun findAllBySeasonIdAndGameIdAndLeague(
        seasonId: Long,
        gameId: Long,
        league: String
    ): Flow<RankingView>



    @Query("""
        SELECT gr.*, (SELECT uid FROM users u WHERE u.id = gr.user_id) user_uid
        FROM game_rankings gr
        WHERE gr.season_id = :seasonId
    """)
    fun findBySeasonId(seasonId: Long): Flow<RankingRow>

}
