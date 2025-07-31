package kr.jiasoft.hiteen.feature.location

data class LocationDto(
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
) {
    fun toHistoryEntity(): LocationHistory =
        LocationHistory(
            userId = userId,
            lat = latitude,
            lng = longitude,
            timestamp = timestamp
        )
}
