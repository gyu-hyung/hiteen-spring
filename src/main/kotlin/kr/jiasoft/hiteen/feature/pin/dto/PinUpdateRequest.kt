package kr.jiasoft.hiteen.feature.pin.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "핀 수정 요청 DTO")
data class PinUpdateRequest(

    @param:Schema(description = "수정할 핀 ID", example = "1")
    val id: Long,               // 수정할 핀 ID

    @param:Schema(description = "우편번호", example = "12345")
    val zipcode: String?,

    @param:Schema(description = "위도", example = "37.5665")
    val lat: Double?,

    @param:Schema(description = "경도", example = "126.9780")
    val lng: Double?,

    @param:Schema(description = "핀 설명", example = "학교 앞 버스 정류장")
    val description: String?,

    @param:Schema(description = "핀 카테고리", example = "CAFE")
    val type: String?,          // 카테고리

    @param:Schema(
        description = "핀 공개 범위",
        example = "FRIENDS",
        allowableValues = ["PUBLIC", "PRIVATE", "FRIENDS"]
    )
    val visibility: String?,     // PUBLIC / PRIVATE / FRIENDS

    @param:Schema(
        description = "FRIENDS 공개 범위일 때 허용할 친구 UID 목록",
        example = "[\"550e8400-e29b-41d4-a716-446655440000\", \"660e8400-e29b-41d4-a716-446655440111\"]"
    )
    val friendUids: List<UUID>? // FRIENDS일 때만 사용
)
