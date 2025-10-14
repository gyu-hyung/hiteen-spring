package kr.jiasoft.hiteen.feature.point.app

import kotlinx.coroutines.runBlocking
import kr.jiasoft.hiteen.feature.point.domain.PointPolicy
import kr.jiasoft.hiteen.feature.point.infra.PointRepository
import kr.jiasoft.hiteen.feature.point.infra.PointSummaryRepository
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PointServiceTest @Autowired constructor(
    private val pointService: PointService,
    private val pointRepository: PointRepository,
    private val pointSummaryRepository: PointSummaryRepository,
) {

    private val testUserId = 1L

    @BeforeEach
    fun setup() = runBlocking {
        pointRepository.deleteAll()
        pointSummaryRepository.deleteAll()
    }

    @Test
    fun `포인트 적립 - 신규 유저 summary 생성`() = runBlocking {
        // given
        val policy = PointPolicy.AD_REWARD // +100

        // when
        val point = pointService.applyPolicy(testUserId, policy)

        // then
        val summary = pointSummaryRepository.findById(testUserId)
        assert(summary != null)
        assert(summary!!.totalPoint == 100)
        assert(point.point == 100)
    }

    @Test
    fun `포인트 누적 - 여러 번 적립 시 합산되어야 한다`() = runBlocking {
        // given
        val policy = PointPolicy.AD_REWARD // +100

        // when
        repeat(3) {
            pointService.applyPolicy(testUserId, policy)
        }

        // then
        val summary = pointSummaryRepository.findById(testUserId)
        assert(summary!!.totalPoint == 300)
    }

    @Test
    fun `포인트 차감 시 summary에서 감산된다`() = runBlocking {
        // given
        pointService.applyPolicy(testUserId, PointPolicy.AD_REWARD) // +100
        pointService.applyPolicy(testUserId, PointPolicy.AD_REWARD) // +100

        // when
        pointService.applyPolicy(testUserId, PointPolicy.GAME_PLAY) // -100

        // then
        val summary = pointSummaryRepository.findById(testUserId)
        assert(summary!!.totalPoint == 100)
    }

    @Test
    fun `잔액 부족 시 예외 발생`() = runBlocking {
        assertThrows<IllegalStateException> {
            runBlocking {
                pointService.applyPolicy(testUserId, PointPolicy.GAME_PLAY) // -100, but 0 balance
            }
        }
    }

    @Test
    fun `일일 제한 초과 시 예외 발생`() = runBlocking {
        val policy = PointPolicy.AD_REWARD // dailyLimit = 5
        repeat(5) { pointService.applyPolicy(testUserId, policy) }

        assertThrows<IllegalStateException> {
            runBlocking {
                pointService.applyPolicy(testUserId, policy)
            }
        }
    }

    @Test
    fun `날짜 범위 조회 정상 작동`() = runBlocking {
        // given
        pointService.applyPolicy(testUserId, PointPolicy.AD_REWARD)
        pointService.applyPolicy(testUserId, PointPolicy.AD_REWARD)
        pointService.applyPolicy(testUserId, PointPolicy.GAME_PLAY)

        // when
        val start = LocalDate.now().minusDays(1)
        val end = LocalDate.now().plusDays(1)
        val result = pointService.getUserPointHistory(testUserId, start, end)

        // then
        assert(result.size == 3)
    }
}
