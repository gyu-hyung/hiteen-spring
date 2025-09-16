package kr.jiasoft.hiteen.feature.sms.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table(name = "sms")
data class SmsEntity(

    @Id
    val id: Long = 0,
    val title: String? = null,
    val content: String? = null,
    val callback: String,
    val total: Long = 0,
    val success: Long? = null,
    val failure: Long? = null,
    val createdId: Long? = null,
    @Column("created_at")
    val createdAt: OffsetDateTime,
    @Column("updated_at")
    val updatedAt: OffsetDateTime? = null,
)