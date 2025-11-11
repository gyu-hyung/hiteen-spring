package kr.jiasoft.hiteen.feature.interest.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("interest_match_history")
data class InterestMatchHistoryEntity (
    @Id
    val id: Long = 0,
    @JsonIgnore
    val userId: Long,
    @JsonIgnore
    val targetId: Long,

    @param:Schema(description = "상태", example = "RECOMMENDED | PASSED")
    val status: String,

    @param:Schema(description = "생성 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val createdAt: OffsetDateTime,
)
