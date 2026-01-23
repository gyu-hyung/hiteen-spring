package kr.jiasoft.hiteen.feature.dashboard.dto

import java.time.LocalDate

data class DashboardResponse(
    val totalUserCount: Long,
    val todayStats: PeriodStat,
    val monthStats: PeriodStat,

    // 가입자 추이 (그래프용)
    // - current: 현재 기간 시계열
    // - previous: 직전 기간 시계열 (예: 현재 30일 vs 직전 30일)
    val joinStats: JoinStatsBundle,

    val schoolTypeStats: List<DashboardSchoolTypeStat>,
    val regionStats: List<RegionStat>
)

data class PeriodStat(
    val count: Long,
    val previousCount: Long,
    val changeRate: Double, // percentage
    val label: String // "전날(342명) 대비" or "전달(24,202명) 대비"
)

data class DailyJoinStat(
    val date: LocalDate,
    val count: Long
)

data class MonthlyJoinStat(
    val yearMonth: String, // "2025-01"
    val count: Long
)

data class YearlyJoinStat(
    val year: String, // "2025"
    val count: Long
)

data class DashboardSchoolTypeStat(
    val type: Int,
    val label: String,
    val count: Long,
    val percentage: Double
)

data class RegionStat(
    val regionCode: String,
    val label: String,
    val count: Long
)

data class JoinStatsBundle(
    val daily: CompareSeries<DailyJoinStat>,
    val monthly: CompareSeries<MonthlyJoinStat>,
    val yearly: CompareSeries<YearlyJoinStat>
)

data class CompareSeries<T>(
    val current: List<T>,
    val previous: List<T>
)
