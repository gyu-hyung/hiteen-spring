package kr.jiasoft.hiteen.feature.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import kr.jiasoft.hiteen.validation.ValidPassword

@Schema(description = "비밀번호 재설정 요청 DTO")
data class ResetPasswordRequest(
    @Schema(description = "휴대폰 번호") val phone: String,
    @Schema(description = "인증번호") val code: String,

    @field:NotBlank(message = "비밀번호는 필수입니다.")
//    @field:ValidPassword
    @Schema(description = "비밀번호", example = "P@ssw0rd!")
    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).{6,}$",
        message = "비밀번호는 영문과 숫자를 포함한 6자 이상이어야 합니다."
    )
    val newPassword: String?
)