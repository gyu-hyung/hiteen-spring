package kr.jiasoft.hiteen.feature.auth.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "로그인 요청 DTO")
data class LoginForm(
    @Schema(description = "아이디") val phone: String,
    @Schema(description = "비밀번호") val password: String
)