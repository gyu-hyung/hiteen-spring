package kr.jiasoft.hiteen.feature.point.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("user_points_summary")
data class PointSummaryEntity(
    @Id
    val userId: Long,
    val totalPoint: Int,
    val updatedAt: OffsetDateTime? = null
)
