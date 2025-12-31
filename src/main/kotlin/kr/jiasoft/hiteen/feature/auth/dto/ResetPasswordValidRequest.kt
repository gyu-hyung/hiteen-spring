package kr.jiasoft.hiteen.feature.auth.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "비밀번호 재설정 요청 DTO")
    data class ResetPasswordValidRequest(
        @field:Schema(description = "휴대폰 번호") val phone: String,
        @field:Schema(description = "인증번호") val code: String,
    )