package kr.jiasoft.hiteen.feature.play.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.play.domain.GameHistoryEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface GameHistoryRepository : CoroutineCrudRepository<GameHistoryEntity, Long> {

    suspend fun findByUid(uid: UUID): GameHistoryEntity?

    //오늘 실행한 게임 목록 조회
    //AND status = 'DONE'
    @Query("""
        SELECT * 
        FROM game_history 
        WHERE to_char(created_at, 'YYYY-MM-DD') = to_char(now(), 'YYYY-MM-DD')
        AND participant_id = :participantId
        AND season_id = :seasonId
        AND game_id = :gameId
        
    """)
    fun listToday(gameId: Long, participantId: Long, seasonId: Long): Flow<GameHistoryEntity>

    suspend fun findByUidAndSeasonIdAndParticipantIdAndGameId(uid: UUID, seasonId: Long, participantId: Long, gameId: Long): GameHistoryEntity?

    suspend fun findTop1ByParticipantIdAndGameIdAndStatusOrderByCreatedAtDesc(
        participantId: Long,
        gameId: Long,
        status: String
    ): GameHistoryEntity?
}
