package kr.jiasoft.hiteen.feature.poll.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("poll_templates")
data class PollTemplateEntity (
    @Id
    val id: Long? = null,
    val question: String?,
    val answers: String?,
    val status: Int?,
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?,
    val deletedAt: OffsetDateTime?,
)