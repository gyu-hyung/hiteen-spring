package kr.jiasoft.hiteen.feature.dashboard.app

import kotlinx.coroutines.runBlocking
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class DashboardScheduler(
    private val dashboardService: DashboardService
) {

    /**
     * 매일 오전 0시 5분에 통계 갱신
     */
    @Scheduled(cron = "0 5 0 * * *")
    @SchedulerLock(name = "refreshDailyStatistics", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    fun refreshDailyStatistics() {
        runBlocking {
            dashboardService.refreshStatistics()
        }
    }
}
