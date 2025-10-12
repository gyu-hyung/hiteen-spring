package kr.jiasoft.hiteen.feature.contact.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("user_contacts")
data class UserContactEntity(
    @Id
    val id: Long? = null,
    val userId: Long,
    val phone: String,
    val name: String? = null,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now()
)
