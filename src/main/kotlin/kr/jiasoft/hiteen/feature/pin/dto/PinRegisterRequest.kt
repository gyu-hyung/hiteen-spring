package kr.jiasoft.hiteen.feature.pin.dto

import java.util.UUID

data class PinRegisterRequest(
    val zipcode: String?,
    val lat: Double,
    val lng: Double,
    val description: String,
    val type: String?,         // 카테고리
    val visibility: String,    // PUBLIC / PRIVATE / FRIENDS
    val friendUids: List<UUID>? // FRIENDS일 때만 사용
)
