package kr.jiasoft.hiteen.feature.location.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive

data class LocationRequest(

    @param:Schema(description = "위도", example = "37.5666")
    @field:Min(-90) @field:Max(90)
    val lat: Double,

    @param:Schema(description = "경도", example = "127.0000")
    @field:Min(-180) @field:Max(180)
    val lng: Double,

    @param:Schema(description = "시간", example = "1726546546546")
    @field:Positive
    val timestamp: Long
)