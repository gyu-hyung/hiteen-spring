package kr.jiasoft.hiteen.feature.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import kr.jiasoft.hiteen.validation.ValidPassword

@Schema(description = "비밀번호 변경 요청 DTO")
data class PassWordChangeRequest(
    @field:NotBlank(message = "비밀번호는 필수입니다.")
    @field:ValidPassword
    @Schema(description = "비밀번호", example = "P@ssw0rd!")
    val oldPassword: String,

    @field:NotBlank(message = "비밀번호는 필수입니다.")
    @field:ValidPassword
    @Schema(description = "비밀번호", example = "P@ssw0rd!")
    val newPassword: String,
)