package kr.jiasoft.hiteen.feature.play.infra

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
          AND s.status = 'ACTIVE'
        LIMIT 1
        """
    )
    suspend fun findActiveParticipant(userId: Long): SeasonParticipantEntity?

}