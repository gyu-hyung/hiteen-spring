package kr.jiasoft.hiteen.feature.location.dto

import kr.jiasoft.hiteen.feature.location.domain.LocationHistory

data class LocationEvent(
    val userId: String,
    val lat: Double,
    val lng: Double,
    val timestamp: Long,
    val source: String = "http"
) {
//    companion object {
//        fun from(h: LocationHistory) =
//            LocationEvent(h.userId, h.lat, h.lng, h.timestamp, "http")
//    }
}