package kr.jiasoft.hiteen.feature.location.domain

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "location_history")
data class LocationHistory(

    @param:Schema(description = "위치 이력 ID", example = "1")
    @Id
    val id: String? = null,

    @param:Schema(description = "사용자 ID", example = "a1a8990f-2443-4492-baad-699d59b272fa")
    val userId: String,

    @param:Schema(description = "위도", example = "37.5666")
    val lat: Double,

    @param:Schema(description = "경도", example = "127.0000")
    val lng: Double,

    @param:Schema(description = "시간", example = "1726546546546")
    val timestamp: Long,
)