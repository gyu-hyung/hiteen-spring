package kr.jiasoft.hiteen.feature.user.app

import jakarta.validation.Valid
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.domain.toResponse
import kr.jiasoft.hiteen.feature.user.dto.UserRegisterForm
import kr.jiasoft.hiteen.feature.user.dto.UserResponse
import kr.jiasoft.hiteen.feature.user.dto.UserUpdateForm
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user")
class UserController(
    private val userService: UserService,
) {

    /** 닉네임 중복 조회 */
    @GetMapping("/nickname/{nickname}")
    suspend fun nicknameDuplicationCheck(@PathVariable nickname: String): ResponseEntity<ApiResult<Boolean>> {
        val exists = userService.nicknameDuplicationCheck(nickname)
        return ResponseEntity.ok(
            ApiResult(
                success = true,
                data = exists,
                message = if (exists) "이미 사용 중인 닉네임입니다." else "사용 가능한 닉네임입니다."
            )
        )
    }

    /** 회원가입 */
    @PostMapping
    suspend fun register(
        @Valid userRegisterForm: UserRegisterForm,
        @RequestPart("file", required = false) file: FilePart?
    ): ResponseEntity<ApiResult<UserResponse>> {
        val user = userService.register(userRegisterForm, file)
        return ResponseEntity.ok(ApiResult(success = true, data = user, message = "회원가입 완료"))
    }

    /** 회원정보 조회 */
    @GetMapping("/me")
    suspend fun me(@AuthenticationPrincipal(expression = "user") user: UserEntity): ResponseEntity<ApiResult<UserResponse>> {
        return ResponseEntity.ok(
            ApiResult(success = true, data = user.toResponse(), message = "회원정보 조회 성공")
        )
    }

    /** 회원정보 수정 */
    @PostMapping("/me/update")
    suspend fun update(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Valid userUpdateForm: UserUpdateForm,
        @RequestPart("file", required = false) file: FilePart?
    ): ResponseEntity<ApiResult<UserResponse>> {
        val updated = userService.updateUser(user, userUpdateForm, file)
        return ResponseEntity.ok(
            ApiResult(success = true, data = updated, message = "회원정보 수정 완료")
        )
    }
}
