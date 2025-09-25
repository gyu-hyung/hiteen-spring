package kr.jiasoft.hiteen.feature.level.domain

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "티어 엔티티")
@Table("tiers")
data class TierEntity(

    @field:Schema(description = "티어 ID (PK)", example = "1")
    @Id
    val id: Long = 0,

    @field:Schema(
        description = "티어 코드 (영문 시스템 코드)",
        example = "BRONZE_STAR"
    )
    val tierCode: String,

    @field:Schema(
        description = "티어 한글 이름",
        example = "별빛 브론즈"
    )
    val tierNameKr: String,

    @field:Schema(
        description = "세부 구분 번호 (1=별빛, 2=달빛, 3=태양)",
        example = "1"
    )
    val divisionNo: Int,

    @field:Schema(
        description = "전체 등급 순서 (1=브론즈1, 2=브론즈2 ...)",
        example = "3"
    )
    val rankOrder: Int,

    @field:Schema(
        description = "티어 상태",
        example = "ACTIVE",
        allowableValues = ["ACTIVE", "INACTIVE"]
    )
    val status: String = "ACTIVE",

    @field:Schema(
        description = "해당 티어 최소 포인트",
        example = "0"
    )
    val minPoints: Int,

    @field:Schema(
        description = "해당 티어 최대 포인트",
        example = "199"
    )
    val maxPoints: Int,

    @field:Schema(
        description = "티어 UUID (고유 식별자)",
        example = "123e4567-e89b-12d3-a456-426614174000"
    )
    val uid: UUID,

    @field:Schema(
        description = "생성 일시",
        example = "2025-09-23T10:45:16+09:00"
    )
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @field:Schema(
        description = "수정 일시",
        example = "2025-09-23T12:10:45+09:00"
    )
    val updatedAt: OffsetDateTime? = null,

    @field:Schema(
        description = "삭제 일시",
        example = "2025-09-23T13:20:30+09:00"
    )
    val deletedAt: OffsetDateTime? = null,
)
