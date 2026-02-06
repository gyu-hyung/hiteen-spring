package kr.jiasoft.hiteen.feature.article.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.OffsetDateTime

@Table("articles")
data class ArticleEntity(
    @Id
    val id: Long = 0,
    val category: String,
    val subject: String?,
    val content: String?,
    val link: String? = null,
    val ip: String? = null,
    val hits: Int = 0,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val status: String,
    val createdId: Long,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedId: Long? = null,
    val updatedAt: OffsetDateTime? = null,
    val deletedId: Long? = null,
    val deletedAt: OffsetDateTime? = null,
)

