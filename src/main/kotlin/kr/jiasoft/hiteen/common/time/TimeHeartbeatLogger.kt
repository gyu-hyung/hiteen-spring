package kr.jiasoft.hiteen.common.time

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * k8s 환경에서 실제 서버 기준 시간이 어떻게 동작하는지 확인하기 위한 1초 주기 로그.
 * - UTC(Instant)
 * - JVM 기본 타임존 기준 OffsetDateTime.now()
 * - KST(Asia/Seoul) 기준 OffsetDateTime
 */
@Component
class TimeHeartbeatLogger {
    private val log = LoggerFactory.getLogger(TimeHeartbeatLogger::class.java)

    @Scheduled(fixedRate = 1000)
    fun heartbeat() {
        val systemZone = ZoneId.systemDefault()
        val utcInstant = Instant.now()
        val systemNow = OffsetDateTime.now()
        val kstNow = OffsetDateTime.now(ZoneId.of("Asia/Seoul"))

        log.info(
            "[TIME_HEARTBEAT] utc={} systemZone={} systemNow={} kstNow={}",
            utcInstant,
            systemZone,
            systemNow,
            kstNow,
        )
    }
}
