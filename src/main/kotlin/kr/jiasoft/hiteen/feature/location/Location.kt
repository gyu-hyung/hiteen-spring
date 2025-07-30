package kr.jiasoft.hiteen.feature.location

import org.springframework.data.annotation.Id

data class Location(
    @Id val id: String? = null,
    val userId: String,
    val lat: Double,
    val lng: Double,
    val timestamp: Long
)
