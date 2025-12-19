package kr.jiasoft.hiteen.feature.cash.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("cash")
data class CashEntity(
    @Id
    val id: Long = 0L,
    val userId: Long,
    val cashableType: String? = null,   // 이벤트 타입 (AD, GAME, PAYMENT 등)
    val cashableId: Long? = null,       // 연관 데이터 ID
    val type: String,                    // CREDIT(적립) / DEBIT(차감)
    val amount: Int,
    val memo: String? = null,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val deletedAt: OffsetDateTime? = null
)