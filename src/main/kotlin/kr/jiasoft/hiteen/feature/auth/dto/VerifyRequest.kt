package kr.jiasoft.hiteen.feature.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class VerifyRequest(

    @param:Schema(description = "휴대폰번호", example = "01012345678")
    @field:NotBlank(message = "휴대폰번호를 입력해줘~")
    val phone: String,

    @param:Schema(description = "인증번호", example = "123456")
    @field:NotBlank(message = "인증번호를 입력해줘~")
    @field:Size(min = 6, max = 6, message = "인증번호는 6자리야~")
    val code: String
)