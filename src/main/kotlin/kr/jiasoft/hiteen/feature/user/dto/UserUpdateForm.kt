package kr.jiasoft.hiteen.feature.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size
import kr.jiasoft.hiteen.validation.ValidPassword
import java.time.LocalDate
import java.util.UUID

data class UserUpdateForm(

    val username: String? = null,

    @field:Email(message = "올바른 이메일 형식이어야 합니다.")
    val email: String? = null,

    @field:Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
    val nickname: String? = null,

    // 값이 존재하지 않을 시 기존 데이터 유지
    @field:ValidPassword
    val password: String? = null,

    val address: String? = null,

    val detailAddress: String? = null,

    val phone: String? = null,

    val mood: String? = null,

    val tier: String? = null,

    val assetUid: UUID? = null,

    val schoolId: Long? = null,

    val grade: String? = null,

    val gender: String? = null,

    val birthday: LocalDate? = null,
)
