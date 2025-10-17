package kr.jiasoft.hiteen.feature.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.auth.dto.JwtResponse

@Schema(description = "회원가입 반환 DTO")
data class UserResponseWithTokens (
    @field:Schema(description = "토큰 정보")
    val tokens: JwtResponse,
    @field:Schema(description = "회원 정보")
    val userResponse: UserResponse,
)