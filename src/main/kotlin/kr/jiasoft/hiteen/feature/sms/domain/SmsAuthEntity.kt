package kr.jiasoft.hiteen.feature.sms.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table(name = "sms_auth")
data class SmsAuthEntity(
    @Id
    val id: Long? = null,
    val smsId: Long? = null,
    val phone: String? = null,
    val code: String? = null,
    val status: String? = null,
    val createdAt: OffsetDateTime? = null,
    val updatedAt: OffsetDateTime? = null,
    val deletedAt: OffsetDateTime? = null,
)