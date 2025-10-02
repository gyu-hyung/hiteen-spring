package kr.jiasoft.hiteen.feature.ad.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("admob_rewards")
data class AdmobRewardEntity(
    @Id
    val id: Long = 0L,
    val transactionId: String,
    val userId: Long,
    val reward: Int,
    val rawData: String? = null,
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
