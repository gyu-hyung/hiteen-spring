package kr.jiasoft.hiteen.feature.session.app

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kr.jiasoft.hiteen.feature.level.app.ExpService
import kr.jiasoft.hiteen.feature.session.domain.SessionStatus
import kr.jiasoft.hiteen.feature.session.domain.UserSession
import kr.jiasoft.hiteen.feature.session.infra.UserSessionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SessionServiceTest {

    private val userSessionRepository: UserSessionRepository = mockk()
    private val expService: ExpService = mockk(relaxed = true)
    private val sessionService = SessionService(userSessionRepository, expService)

    @Test
    fun `세션 시작 테스트`() = runTest {
        // given
        val now = LocalDateTime.now()
        val mockSession = UserSession(
            id = 1L,
            userId = 100L,
            startTime = now,
            status = SessionStatus.ACTIVE
        )

        coEvery { userSessionRepository.save(any()) } returns mockSession

        // when
        val result = sessionService.startSession(100L)

        // then
        assertEquals(100L, result.userId)
        assertEquals(SessionStatus.ACTIVE, result.status)
        coVerify { userSessionRepository.save(any()) }
    }

    @Test
    fun `세션 종료 시 경험치 적립 호출`() = runTest {
        // given
        val startTime = LocalDateTime.now().minusMinutes(10)
        val activeSession = UserSession(
            id = 1L,
            userId = 100L,
            startTime = startTime,
            status = SessionStatus.ACTIVE
        )
        val closedSession = activeSession.copy(
            endTime = LocalDateTime.now(),
            durationMinutes = 10,
            status = SessionStatus.CLOSED
        )

        coEvery { userSessionRepository.findTopByUserIdAndStatusOrderByStartTimeDesc(100L, SessionStatus.ACTIVE) } returns activeSession
        coEvery { userSessionRepository.save(any()) } returns closedSession

        // when
        sessionService.endSession(100L)

        // then
        coVerify { expService.grantSessionExp(100L, 1L, 10) }
    }
}
