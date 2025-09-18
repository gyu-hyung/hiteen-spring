package kr.jiasoft.hiteen.feature.interest.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "사용자 관심사 응답 DTO")
@Table("interest_user")
data class InterestUserResponse(

    @param:Schema(description = "관심사 매핑 고유 ID", example = "101")
    @Id
    val id: Long,

    @JsonIgnore
    @param:Schema(description = "사용자 ID (내부 식별용)", example = "5", hidden = true)
    val userId: Long,

    @param:Schema(description = "관심사 주제", example = "헬스")
    val topic: String,

    @param:Schema(description = "관심사 카테고리", example = "운동")
    val category: String,

    @param:Schema(description = "상태", example = "ACTIVE")
    val status: String?,

    @param:Schema(description = "사용자 UID", example = "c264013d-bb1d-4d66-8d34-10962c022056")
    val userUid: UUID,

    @param:Schema(description = "관심사 ID", example = "12")
    val interestId: Long,

    @param:Schema(description = "생성 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val createdAt: OffsetDateTime,
)
