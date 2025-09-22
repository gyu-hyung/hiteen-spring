package kr.jiasoft.hiteen.feature.interest.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.interest.domain.InterestEntity
import java.time.OffsetDateTime

@Schema(description = "관심사 응답 DTO")
data class InterestResponse(

    @field:Schema(description = "관심사 ID", example = "1")
    val id: Long,

    @field:Schema(description = "관심사 주제", example = "헬스")
    val topic: String,

    @field:Schema(description = "관심사 카테고리", example = "운동")
    val category: String,

    @field:Schema(description = "내 관심사로 등록 여부 (Y=등록됨, N=미등록)", example = "Y")
    val status: String,

    @field:Schema(description = "생성 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val createdAt: OffsetDateTime,

    @field:Schema(description = "수정 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val updatedAt: OffsetDateTime? = null,

    @field:Schema(description = "삭제 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val deletedAt: OffsetDateTime? = null
)

fun InterestEntity.toResponse(): InterestResponse =
    InterestResponse(
        id = this.id,
        topic = this.topic,
        category = this.category,
        status = if (this.status == "Y") "Y" else "N",
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        deletedAt = this.deletedAt
    )
