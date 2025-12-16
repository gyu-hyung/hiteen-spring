package kr.jiasoft.hiteen.feature.cash.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("user_cash_summary")
data class CashSummaryEntity(
    @Id
    val userId: Long,
    val totalPoint: Int,
    val updatedAt: OffsetDateTime? = null
)
