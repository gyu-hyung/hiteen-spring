package kr.jiasoft.hiteen.feature.sms.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table(name = "sms_details")
data class SmsDetailEntity (
    @Id
    val id: Long = 0,
    val smsId: Long,
    val phone: String,
    val success: Boolean,
    val error: String? = null,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime? = null,
)