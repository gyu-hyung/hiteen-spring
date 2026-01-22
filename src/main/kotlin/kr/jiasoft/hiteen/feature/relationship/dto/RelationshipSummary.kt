package kr.jiasoft.hiteen.feature.relationship.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.relationship.domain.LocationMode
import kr.jiasoft.hiteen.feature.user.dto.UserResponse
import java.time.OffsetDateTime

@Schema(description = "관계 요약 정보")
data class RelationshipSummary(

    @param:Schema(description = "유저 요약 정보")
    val userResponse: UserResponse,

    @param:Schema(description = "상태", example = "PENDING")
    val status: String,          // PENDING / ACCEPTED / ...

    @param:Schema(description = "상태 변경 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val statusAt: OffsetDateTime?,

    @param:Schema(description = "내 위치 모드", example = "PUBLIC")
    val myLocationMode: LocationMode? = null,

    @param:Schema(description = "상대 위치 모드", example = "PUBLIC")
    val theirLocationMode: LocationMode? = null,

    @param:Schema(description = "위도", example = "37.5666")
    val lat: Double? = null,

    @param:Schema(description = "경도", example = "127.0000")
    val lng: Double? = null,

    @param:Schema(description = "마지막 위치 확인 시간", example = "2025.09.18 10:15:30")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val lastSeenAt: OffsetDateTime? = null,
)