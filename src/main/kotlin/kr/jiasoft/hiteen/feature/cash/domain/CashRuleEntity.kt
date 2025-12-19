package kr.jiasoft.hiteen.feature.cash.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.threeten.bp.OffsetDateTime

@Table("cash_rules")
data class CashRuleEntity(
    @Id
    val id: Long = 0,
    val actionCode: String,
    val amount: Int,
    val dailyCap: Int?,
    val cooldownSec: Int?,
    val description: String?,
    val deletedAt: OffsetDateTime?
)
