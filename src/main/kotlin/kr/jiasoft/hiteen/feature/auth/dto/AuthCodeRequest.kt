package kr.jiasoft.hiteen.feature.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class AuthCodeRequest(

    @param:Schema(description = "휴대폰번호", example = "01012345678")
    @field:NotBlank(message = "휴대폰번호를 입력해줘~")
    @field:Pattern(regexp = "^[0-9]{9,20}$", message = "올바른 휴대폰번호 형식이 아냐~")
    val phone: String,

    @param:Schema(description = "닉네임", example = "홍길동")
    @field:NotBlank(message = "휴대폰번호를 입력해줘~")
    val nickName: String,

    @param:Schema(description = "인증번호 타입", example = "User")
    val type: String? = "User"
)