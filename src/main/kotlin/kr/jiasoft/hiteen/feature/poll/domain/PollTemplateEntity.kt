package kr.jiasoft.hiteen.feature.poll.domain

import io.r2dbc.postgresql.codec.Json
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("poll_templates")
data class PollTemplateEntity(
    @Id
    val id: Long = 0,
    val question: String,
    val answers: Json,   // JSON 문자열 (["치킨", "피자", "김밥"] 형태)
    val state: Short = 1,  // 1: 활성, 0: 비활성
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime? = null,
    val deletedAt: OffsetDateTime? = null
)