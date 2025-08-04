package kr.jiasoft.hiteen.feature.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size
import kr.jiasoft.hiteen.validation.ValidPassword

data class UserUpdateForm(

//    @field:NotBlank(message = "아이디는 필수입니다.")
    val username: String? = null,

//    @field:NotBlank
    @field:Email(message = "올바른 이메일 형식이어야 합니다.")
    val email: String? = null,

//    @field:NotBlank
    @field:Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
    val nickname: String? = null,

//    @field:NotBlank(message = "비밀번호는 필수입니다.")
    //값이 존재하지않을시 기존 데이터 적용됩니다.
    @field:ValidPassword
    val password: String? = null,
)