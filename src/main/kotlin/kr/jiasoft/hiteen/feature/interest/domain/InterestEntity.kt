package kr.jiasoft.hiteen.feature.interest.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Schema(description = "관심사 엔티티")
@Table("interests")
data class InterestEntity (

    @param:Schema(description = "관심사 ID", example = "1")
    @Id
    val id: Long = 0,

    @param:Schema(description = "관심사 주제", example = "축구")
    val topic: String,

    @param:Schema(description = "관심사 카테고리", example = "스포츠")
    val category: String,

    @param:Schema(
        description = "상태",
        example = "ACTIVE",
        allowableValues = ["ACTIVE", "INACTIVE"]
    )
    val status: String,

    @JsonIgnore
    @param:Schema(hidden = true)
    val createdId: Long,

    @param:Schema(description = "생성 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val createdAt: OffsetDateTime,

    @JsonIgnore
    @param:Schema(hidden = true)
    val updatedId: Long? = null,

    @param:Schema(description = "수정 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val updatedAt: OffsetDateTime? = null,

    @JsonIgnore
    @param:Schema(hidden = true)
    val deletedId: Long? = null,

    @param:Schema(description = "삭제 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val deletedAt: OffsetDateTime? = null
)
