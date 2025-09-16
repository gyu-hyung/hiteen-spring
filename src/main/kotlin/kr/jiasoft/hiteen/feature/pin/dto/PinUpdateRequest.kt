package kr.jiasoft.hiteen.feature.pin.dto

import java.util.UUID

data class PinUpdateRequest(
    val id: Long,               // 수정할 핀 ID
    val zipcode: String?,
    val lat: Double,
    val lng: Double,
    val description: String,
    val type: String?,          // 카테고리
    val visibility: String,     // PUBLIC / PRIVATE / FRIENDS
    val friendUids: List<UUID>? // FRIENDS일 때만 사용
)
