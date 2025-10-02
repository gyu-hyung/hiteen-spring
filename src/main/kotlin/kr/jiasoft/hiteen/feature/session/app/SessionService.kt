package kr.jiasoft.hiteen.feature.session.app

import kr.jiasoft.hiteen.feature.level.app.ExpService
import kr.jiasoft.hiteen.feature.session.domain.SessionStatus
import kr.jiasoft.hiteen.feature.session.domain.UserSession
import kr.jiasoft.hiteen.feature.session.infra.UserSessionRepository
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

@Service
class SessionService(
    private val userSessionRepository: UserSessionRepository,
    private val expService: ExpService
) {

    /**
     * 세션 시작
     */
    suspend fun startSession(userId: Long): UserSession {
        val session = UserSession(
            userId = userId,
            startTime = LocalDateTime.now(),
            status = SessionStatus.ACTIVE
        )
        return userSessionRepository.save(session)
    }

    /**
     * 세션 종료 + 경험치 적립
     */
    suspend fun endSession(userId: Long) {
        val session = userSessionRepository
            .findTopByUserIdAndStatusOrderByStartTimeDesc(userId, SessionStatus.ACTIVE)
            ?: throw IllegalStateException("진행 중인 세션이 없습니다.")

        val endTime = LocalDateTime.now()
        val durationMinutes = Duration.between(session.startTime, endTime).toMinutes().toInt()

        val closedSession = session.copy(
            endTime = endTime,
            durationMinutes = durationMinutes,
            status = SessionStatus.CLOSED
        )
        val savedSession = userSessionRepository.save(closedSession)

        val points = calculateSessionPoints(durationMinutes)
        if (points > 0) {
            expService.grantSessionExp(userId, savedSession.id!!, durationMinutes)
        }
    }

    /**
     * 접속 시간 → 점수 변환
     */
    private fun calculateSessionPoints(minutes: Int): Int =
        when {
            minutes <= 5 -> 5
            minutes <= 15 -> 10
            minutes <= 30 -> 20
            else -> 30
        }
}
