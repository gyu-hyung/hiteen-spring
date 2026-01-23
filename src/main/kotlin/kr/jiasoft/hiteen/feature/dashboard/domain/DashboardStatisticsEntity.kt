package kr.jiasoft.hiteen.feature.dashboard.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.OffsetDateTime

@Table("dashboard_statistics")
data class DashboardStatisticsEntity(
    @Id
    val id: Long? = null,
    val date: LocalDate,
    val totalUserCount: Long = 0,
    val todayJoinCount: Long = 0,
    val schoolType1Count: Long = 0,
    val schoolType2Count: Long = 0,
    val schoolType3Count: Long = 0,
    val schoolType9Count: Long = 0,
    val regionStats: String? = null,
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)

