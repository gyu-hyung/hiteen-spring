package kr.jiasoft.hiteen.feature.study.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("question")
data class QuestionEntity(
    @Id
    val id: Long,
    val type: Int,
    val category: String?,
    val question: String?,
    val symbol: String?,
    val sound: String?,
    val answer: String?,
    val content: String?,
    val status: Int,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val deletedAt: LocalDateTime?
)
