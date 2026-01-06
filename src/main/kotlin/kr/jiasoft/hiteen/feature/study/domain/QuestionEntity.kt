package kr.jiasoft.hiteen.feature.study.domain

import com.fasterxml.jackson.annotation.JsonFormat
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("question_2")
data class QuestionEntity(
    @Id
    val id: Long = 0,
    val type: Int,
    val category: String?,
    val question: String,
    val symbol: String?,
    val answer: String?,
    val sound: String?,
    val image: String?,
    val content: String?,
    val status: Int,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime?,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val updatedAt: OffsetDateTime?,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val deletedAt: OffsetDateTime?
)
