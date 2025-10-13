package kr.jiasoft.hiteen.feature.play.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("question_items")
data class QuestionItemsEntity(
    @Id
    val id: Long = 0,
    val seasonId: Long,
    val type: Int,
    val questionId: Long,
    val answers: String // JSON 배열 문자열 형태 (e.g. ["용기", "차량", "의학", ...])
)
