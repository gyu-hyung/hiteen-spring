package kr.jiasoft.hiteen.feature.point.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("points")
data class PointEntity(
    @Id
    val id: Long = 0L,
    val userId: Long,
    val pointableType: String? = null,   // 이벤트 타입 (AD, GAME, PAYMENT 등)
    val pointableId: Long? = null,       // 연관 데이터 ID
    val type: String,                    // CREDIT(적립) / DEBIT(차감)
    val point: Int,
    val memo: String? = null,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val deletedAt: OffsetDateTime? = null
)