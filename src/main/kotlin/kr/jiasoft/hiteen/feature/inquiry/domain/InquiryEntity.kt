package kr.jiasoft.hiteen.feature.inquiry.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("inquiries")
data class InquiryEntity(
    @Id
    val id: Long = 0,
    val name: String,
    val phone: String,
    val email: String? = null,
    val content: String,
    val ip: String? = null,
    val status: String = InquiryStatus.PENDING.name,
    val replyContent: String? = null,
    val replyAt: OffsetDateTime? = null,
    val replyBy: Long? = null,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime? = null,
)

