package kr.jiasoft.hiteen.feature.dashboard.infra

import kr.jiasoft.hiteen.feature.dashboard.domain.DashboardStatisticsEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface DashboardStatisticsRepository : CoroutineCrudRepository<DashboardStatisticsEntity, Long> {
    suspend fun findByDate(date: LocalDate): DashboardStatisticsEntity?
    suspend fun findAllByDateBetweenOrderByDateAsc(startDate: LocalDate, endDate: LocalDate): List<DashboardStatisticsEntity>
}

