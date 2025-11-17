package kr.jiasoft.hiteen.feature.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

@Schema(description = "비밀번호 재설정 요청 DTO")
data class ChangePhoneRequest(

    @field:NotBlank(message = "연락처는 필수입니다.")
    @field:Pattern(
        regexp = "^01[0-9]{8,9}$",
        message = "올바른 휴대폰 번호 형식이어야 합니다. (예: 01012345678)"
    )
    @Schema(description = "휴대폰 번호")
    val phone: String,

    @field:NotBlank(message = "인증번호는 필수입니다.")
    @Schema(description = "인증번호")
    val code: String,
)