package kr.jiasoft.hiteen.feature.sms.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table(name = "sms_auth")
data class SmsAuthEntity(
    @Id
    val id: Long = 0,
    val smsId: Long,
    val phone: String,
    val code: String,
    val status: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime? = null,
    val deletedAt: OffsetDateTime? = null,
)