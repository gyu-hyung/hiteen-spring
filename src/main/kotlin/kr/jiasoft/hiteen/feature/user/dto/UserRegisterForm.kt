package kr.jiasoft.hiteen.feature.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.validation.ValidPassword
import java.time.LocalDateTime

data class UserRegisterForm(
    @field:NotBlank(message = "아이디는 필수입니다.")
    val username: String? = null,

    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "올바른 이메일 형식이어야 합니다.")
    val email: String? = null,

    @field:NotBlank(message = "닉네임은 필수입니다.")
    @field:Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
    val nickname: String? = null,


    @field:NotBlank(message = "비밀번호는 필수입니다.")
    @field:ValidPassword
    val password: String? = null,
) {
    fun toEntity(encodedPassword: String): UserEntity = UserEntity(
        uid = "",
        username = username?:"",
        email = email,
        nickname = nickname,
        password = encodedPassword,
        role = "USER",
        createdAt = LocalDateTime.now(),
    )
}
