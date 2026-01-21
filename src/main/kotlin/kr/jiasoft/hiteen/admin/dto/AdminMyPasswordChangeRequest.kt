package kr.jiasoft.hiteen.admin.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

@Schema(description = "관리자 비밀번호 변경 요청 DTO")
data class AdminMyPasswordChangeRequest(
    @field:NotBlank(message = "기존 비밀번호를 입력해 주세요.")
    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{6,}$",
        message = "비밀번호는 영문,숫자,특수문자 포함, 6자 이상 입력하세요."
    )
    val oldPassword: String,

    @field:NotBlank(message = "새 비밀번호를 입력해 주세요.")
    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{6,}$",
        message = "비밀번호는 영문,숫자,특수문자 포함, 6자 이상 입력하세요."
    )
    val newPassword: String,

    @field:NotBlank(message = "새 비밀번호를 한 번 더 입력해 주세요.")
    val newPasswordConfirm: String,

    @field:AssertTrue(message = "비밀번호 확인이 일치하지 않습니다.")
    val isPasswordConfirmed: Boolean = newPassword == newPasswordConfirm
)