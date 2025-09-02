package kr.jiasoft.hiteen.feature.location.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive

data class LocationRequest(
    @field:Min(-90) @field:Max(90)
    val lat: Double,
    @field:Min(-180) @field:Max(180)
    val lng: Double,
    @field:Positive
    val timestamp: Long
)