package kr.jiasoft.hiteen.feature.dashboard.app

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.dashboard.dto.*
import kr.jiasoft.hiteen.feature.dashboard.infra.DashboardAggregateRepository
import kr.jiasoft.hiteen.feature.dashboard.infra.DashboardStatisticsRepository
import kr.jiasoft.hiteen.feature.dashboard.domain.DashboardStatisticsEntity
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DashboardService(
    private val aggregateRepository: DashboardAggregateRepository,
    private val statisticsRepository: DashboardStatisticsRepository,
    private val objectMapper: ObjectMapper
) {
    private val schoolTypeLabels = mapOf(
        1 to "초등학교",
        2 to "중학교",
        3 to "고등학교",
        9 to "기타"
    )

    private val schoolAreas = listOf(
        AreaInfo("B10", "서울", "서울특별시교육청"),
        AreaInfo("C10", "부산", "부산광역시교육청"),
        AreaInfo("D10", "대구", "대구광역시교육청"),
        AreaInfo("E10", "인천", "인천광역시교육청"),
        AreaInfo("F10", "광주", "광주광역시교육청"),
        AreaInfo("G10", "대전", "대전광역시교육청"),
        AreaInfo("H10", "울산", "울산광역시교육청"),
        AreaInfo("I10", "세종", "세종특별자치시교육청"),
        AreaInfo("J10", "경기", "경기도교육청"),
        AreaInfo("K10", "강원", "강원특별자치도교육청"),
        AreaInfo("M10", "충북", "충청북도교육청"),
        AreaInfo("N10", "충남", "충청남도교육청"),
        AreaInfo("P10", "전북", "전북특별자치도교육청"),
        AreaInfo("Q10", "전남", "전라남도교육청"),
        AreaInfo("R10", "경북", "경상북도교육청"),
        AreaInfo("S10", "경남", "경상남도교육청"),
        AreaInfo("T10", "제주", "제주특별자치도교육청")
    )

    data class AreaInfo(val value: String, val short: String, val label: String)

    suspend fun getDashboardData(): DashboardResponse {
        val total = aggregateRepository.getTotalUserCount()
        val todayCount = aggregateRepository.getTodayJoinCount()
        val yesterdayCount = aggregateRepository.getYesterdayJoinCount()
        val monthCount = aggregateRepository.getMonthJoinCount()
        val lastMonthCount = aggregateRepository.getLastMonthJoinCount()

        val todayStats = createPeriodStat(todayCount, yesterdayCount, "전날(${yesterdayCount}명) 대비")
        val monthStats = createPeriodStat(monthCount, lastMonthCount, "전달(${lastMonthCount}명) 대비")

        // Get historical stats for charts
        val endDate = LocalDate.now()
        val startDate = endDate.minusYears(2) // previous period 비교까지 고려해 2년치 확보
        var allHistory = statisticsRepository.findAllByDateBetweenOrderByDateAsc(startDate, endDate)

        // Mock data if history is empty
        if (allHistory.isEmpty()) {
            val mockHistory = mutableListOf<DashboardStatisticsEntity>()
            for (i in 730 downTo 1) {
                val mockDate = endDate.minusDays(i.toLong())
                mockHistory.add(
                    DashboardStatisticsEntity(
                        date = mockDate,
                        todayJoinCount = (10L..50L).random(),
                        totalUserCount = 1000L + (i * 10)
                    )
                )
            }
            allHistory = mockHistory
        }

        // --------- Join Stats (그래프용) ---------
        // Daily (현재 30일 vs 직전 30일)
        val dailyWindowDays = 30L
        val currentDailyStart = endDate.minusDays(dailyWindowDays - 1)
        val currentDailyEnd = endDate
        val previousDailyEnd = currentDailyStart.minusDays(1)
        val previousDailyStart = previousDailyEnd.minusDays(dailyWindowDays - 1)

        val historyDailyMap = allHistory.associate { it.date to it.todayJoinCount }

        fun buildDailySeries(start: LocalDate, end: LocalDate): List<DailyJoinStat> {
            val list = mutableListOf<DailyJoinStat>()
            var d = start
            while (!d.isAfter(end)) {
                val count = if (d.isEqual(endDate)) {
                    // 오늘은 live 값 우선
                    todayCount
                } else {
                    historyDailyMap[d] ?: 0
                }
                list.add(DailyJoinStat(d, count))
                d = d.plusDays(1)
            }
            return list
        }

        val dailySeriesCurrent = buildDailySeries(currentDailyStart, currentDailyEnd)
        val dailySeriesPrevious = buildDailySeries(previousDailyStart, previousDailyEnd)

        // Monthly (최근 12개월 vs 이전 12개월)
        val ymOf: (LocalDate) -> String = { it.year.toString() + "-" + it.monthValue.toString().padStart(2, '0') }
        val allHistoryPlusToday = allHistory.plus(DashboardStatisticsEntity(date = endDate, todayJoinCount = todayCount))

        val monthSumMap = allHistoryPlusToday
            .groupBy { ymOf(it.date) }
            .mapValues { (_, stats) -> stats.sumOf { it.todayJoinCount } }

        fun buildMonthlySeries(startYm: LocalDate, months: Int): List<MonthlyJoinStat> {
            val result = mutableListOf<MonthlyJoinStat>()
            var cursor = startYm.withDayOfMonth(1)
            repeat(months) {
                val key = ymOf(cursor)
                result.add(MonthlyJoinStat(key, monthSumMap[key] ?: 0))
                cursor = cursor.plusMonths(1)
            }
            return result
        }

        val currentMonthStart = endDate.withDayOfMonth(1).minusMonths(11)
        val previousMonthStart = currentMonthStart.minusMonths(12)
        val monthlySeriesCurrent = buildMonthlySeries(currentMonthStart, 12)
        val monthlySeriesPrevious = buildMonthlySeries(previousMonthStart, 12)

        // Yearly (최근 5년 vs 5년 전)
        val yearSumMap = allHistoryPlusToday
            .groupBy { it.date.year.toString() }
            .mapValues { (_, stats) -> stats.sumOf { it.todayJoinCount } }

        fun buildYearlySeries(startYear: Int, years: Int): List<YearlyJoinStat> {
            val result = mutableListOf<YearlyJoinStat>()
            for (y in startYear until (startYear + years)) {
                val key = y.toString()
                result.add(YearlyJoinStat(key, yearSumMap[key] ?: 0))
            }
            return result
        }

        val yearlyWindowYears = 5
        val currentYearStart = endDate.year - (yearlyWindowYears - 1)
        // previous는 "직전"이 아니라 "5년 전"(동일 기간을 -5년 시프트)
        val previousYearStart = currentYearStart - yearlyWindowYears

        val yearlySeriesCurrent = buildYearlySeries(currentYearStart, yearlyWindowYears)
        val yearlySeriesPrevious = buildYearlySeries(previousYearStart, yearlyWindowYears)

        val joinStats = JoinStatsBundle(
            daily = CompareSeries(current = dailySeriesCurrent, previous = dailySeriesPrevious),
            monthly = CompareSeries(current = monthlySeriesCurrent, previous = monthlySeriesPrevious),
            yearly = CompareSeries(current = yearlySeriesCurrent, previous = yearlySeriesPrevious)
        )

        // --------- (기존) School/Region stats ---------
        val latestStats = allHistory.lastOrNull { it.regionStats != null }

        val typeStats: List<DashboardSchoolTypeStat>
        val regionStats: List<RegionStat>

        if (latestStats != null && latestStats.regionStats != null) {
            val totalByType = latestStats.schoolType1Count + latestStats.schoolType2Count + latestStats.schoolType3Count + latestStats.schoolType9Count
            typeStats = listOf(
                createDashboardSchoolTypeStat(1, latestStats.schoolType1Count, totalByType),
                createDashboardSchoolTypeStat(2, latestStats.schoolType2Count, totalByType),
                createDashboardSchoolTypeStat(3, latestStats.schoolType3Count, totalByType),
                createDashboardSchoolTypeStat(9, latestStats.schoolType9Count, totalByType)
            )

            val savedRegionMap: Map<String, Long> = objectMapper.readValue(latestStats.regionStats)
            regionStats = schoolAreas.map { area ->
                RegionStat(area.value, area.short, savedRegionMap[area.value] ?: 0)
            }
        } else {
            val typeStatsRaw = aggregateRepository.getSchoolTypeStats().toList()
            val totalByType = typeStatsRaw.sumOf { it.count }
            typeStats = schoolTypeLabels.map { (type, _) ->
                val count = typeStatsRaw.find { it.type == type }?.count ?: 0
                createDashboardSchoolTypeStat(type, count, totalByType)
            }

            val regionStatsRaw = aggregateRepository.getRegionStats().toList()
            regionStats = schoolAreas.map { area ->
                val count = regionStatsRaw.find { it.sido == area.value }?.count ?: 0
                RegionStat(area.value, area.short, count)
            }
        }

        return DashboardResponse(
            totalUserCount = total,
            todayStats = todayStats,
            monthStats = monthStats,
            joinStats = joinStats,
            schoolTypeStats = typeStats,
            regionStats = regionStats
        )
    }

    private fun createPeriodStat(current: Long, previous: Long, label: String): PeriodStat {
        val changeRate = if (previous > 0) {
            ((current - previous).toDouble() / previous * 100)
        } else {
            if (current > 0) 100.0 else 0.0
        }
        return PeriodStat(current, previous, Math.round(changeRate * 10.0) / 10.0, label)
    }

    private fun createDashboardSchoolTypeStat(type: Int, count: Long, total: Long): DashboardSchoolTypeStat {
        val label = schoolTypeLabels[type] ?: "기타"
        val percentage = if (total > 0) (count.toDouble() / total * 100).let { Math.round(it * 10.0) / 10.0 } else 0.0
        return DashboardSchoolTypeStat(type, label, count, percentage)
    }

    suspend fun refreshStatistics(date: LocalDate = LocalDate.now()) {
        val total = aggregateRepository.getTotalUserCount()
        val today = if (date.isEqual(LocalDate.now())) aggregateRepository.getTodayJoinCount() else 0 // Simplified

        val typeStatsRaw = aggregateRepository.getSchoolTypeStats().toList()
        val regionStatsRaw = aggregateRepository.getRegionStats().toList()

        val regionMap = regionStatsRaw.associate { it.sido to it.count }

        val entity = DashboardStatisticsEntity(
            date = date,
            totalUserCount = total,
            todayJoinCount = today,
            schoolType1Count = typeStatsRaw.find { it.type == 1 }?.count ?: 0,
            schoolType2Count = typeStatsRaw.find { it.type == 2 }?.count ?: 0,
            schoolType3Count = typeStatsRaw.find { it.type == 3 }?.count ?: 0,
            schoolType9Count = typeStatsRaw.find { it.type == 9 }?.count ?: 0,
            regionStats = objectMapper.writeValueAsString(regionMap)
        )

        val existing = statisticsRepository.findByDate(date)
        if (existing != null) {
            statisticsRepository.save(entity.copy(id = existing.id))
        } else {
            statisticsRepository.save(entity)
        }
    }
}
