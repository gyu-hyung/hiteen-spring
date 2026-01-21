package kr.jiasoft.hiteen.feature.point.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("point_rules")
data class PointRuleEntity(
    @Id
    val id: Long = 0,
    val actionCode: String,
    val point: Int,
    val dailyCap: Int?,
    val cooldownSec: Int?,
    val description: String?,
    val deletedAt: OffsetDateTime?
)
