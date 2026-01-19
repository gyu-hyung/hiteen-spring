package kr.jiasoft.hiteen.common.time

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/**
 * 앱에서 사용하는 기준 시간.
 * 출석/일자 계산은 KST(Asia/Seoul) 기준으로 고정해서 UTC 서버에서도 날짜 경계 이슈를 방지한다.
 */
@Component
class AppClock {
    private val zoneId: ZoneId = ZoneId.of("Asia/Seoul")
    private val clock: Clock = Clock.system(zoneId)

    fun todayKst(): LocalDate = LocalDate.now(clock)
    fun zoneId(): ZoneId = zoneId
}

