package kr.jiasoft.hiteen.feature.sms.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table(name = "sms_details")
data class SmsDetailEntity (
    @Id
    val id: Long? = null,
    val smsId: Long? = null,
    val phone: String? = null,
    val success: Boolean? = null,
    val error: String? = null,
    val createdAt: OffsetDateTime? = null,
    val updatedAt: OffsetDateTime? = null,
)