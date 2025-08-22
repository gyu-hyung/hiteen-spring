package kr.jiasoft.hiteen.feature.location.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "location_history")
data class LocationHistory(
    @Id
    val id: String? = null,
    val userId: String,
    val lat: Double,
    val lng: Double,
    val timestamp: Long,
)