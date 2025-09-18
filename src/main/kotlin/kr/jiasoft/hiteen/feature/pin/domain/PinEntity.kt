package kr.jiasoft.hiteen.feature.pin.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Schema(description = "위치 핀 Entity")
@Table(name = "pin")
data class PinEntity(

    @param:Schema(description = "핀 고유 ID", example = "1")
    @Id
    val id: Long = 0,

    @param:Schema(description = "핀을 등록한 사용자 ID", example = "1001")
    val userId: Long,

    @param:Schema(description = "우편번호", example = "12345")
    val zipcode: String?,

    @param:Schema(description = "위도", example = "37.5665")
    val lat: Double,

    @param:Schema(description = "경도", example = "126.9780")
    val lng: Double,

    @param:Schema(description = "핀 설명", example = "학교 앞 버스 정류장")
    val description: String,

    @param:Schema(description = "핀 공개 범위 (예: PUBLIC, FRIENDS, PRIVATE)", example = "PUBLIC")
    val visibility: String,

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

    @param:Schema(description = "삭제 처리한 사용자 ID", example = "1002")
    val deletedId: Long? = null,

    @param:Schema(description = "삭제 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val deletedAt: OffsetDateTime? = null,
)
