package kr.jiasoft.hiteen.feature.auth.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class AuthCodeRequest(
    @field:NotBlank(message = "휴대폰번호를 입력해줘~")
    @field:Pattern(regexp = "^[0-9]{9,20}$", message = "올바른 휴대폰번호 형식이 아냐~")
    val phone: String,

    val type: String? = "User"
)

data class VerifyRequest(
    @field:NotBlank(message = "휴대폰번호를 입력해줘~")
    val phone: String,

    @field:NotBlank(message = "인증번호를 입력해줘~")
    @field:Size(min = 6, max = 6, message = "인증번호는 6자리야~")
    val code: String
)