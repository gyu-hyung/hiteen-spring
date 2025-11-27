package kr.jiasoft.hiteen.feature.play.infra

import kr.jiasoft.hiteen.feature.play.domain.GameHistoryEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface GameHistoryRepository : CoroutineCrudRepository<GameHistoryEntity, Long> {

    suspend fun findByUid(uid: UUID): GameHistoryEntity?


    suspend fun findByUidAndSeasonIdAndParticipantIdAndGameId(uid: UUID, seasonId: Long, participantId: Long, gameId: Long): GameHistoryEntity?

    suspend fun findTop1ByParticipantIdAndGameIdAndStatusOrderByCreatedAtDesc(
        participantId: Long,
        gameId: Long,
        status: String
    ): GameHistoryEntity?
}
