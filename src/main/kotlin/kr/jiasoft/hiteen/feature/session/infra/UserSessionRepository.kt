package kr.jiasoft.hiteen.feature.session.infra

import kr.jiasoft.hiteen.feature.session.domain.SessionStatus
import kr.jiasoft.hiteen.feature.session.domain.UserSession
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UserSessionRepository : CoroutineCrudRepository<UserSession, Long> {
    suspend fun findTopByUserIdAndStatusOrderByStartTimeDesc(userId: Long, status: SessionStatus): UserSession?
}

