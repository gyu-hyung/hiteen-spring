package kr.jiasoft.hiteen.feature.auth.dto

import io.swagger.v3.oas.annotations.media.Schema

data class JwtResponse(
    @Schema(description = "Access Token") val accessToken: String,
    @Schema(description = "Access Token") val refreshToken: String? = null
)
