package kr.jiasoft.hiteen.feature.play.infra

import kr.jiasoft.hiteen.feature.play.domain.RewardLeagueStartNotificationEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface RewardLeagueStartNotificationRepository : CoroutineCrudRepository<RewardLeagueStartNotificationEntity, Long> {

    /**
     * (season, league, game) 기준으로 이미 알림 트리거 레코드가 있는지 확인
     */
    suspend fun existsBySeasonIdAndLeagueAndGameId(seasonId: Long, league: String, gameId: Long): Boolean

    /**
     * 해당 시즌+리그+게임의 점수 등록자 수(= game_scores row 수). league는 season_participants.league 기준.
     */
    @Query(
        """
        SELECT COUNT(*)
        FROM game_scores gs
        JOIN season_participants sp ON sp.id = gs.participant_id
        WHERE gs.season_id = :seasonId
          AND gs.game_id = :gameId
          AND sp.league = :league
        """
    )
    suspend fun countScoreParticipants(seasonId: Long, league: String, gameId: Long): Long

    @Query("SELECT name FROM games WHERE id = :gameId")
    suspend fun findGameName(gameId: Long): String?
}
