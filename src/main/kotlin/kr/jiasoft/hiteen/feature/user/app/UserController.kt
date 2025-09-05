package kr.jiasoft.hiteen.feature.user.app

import jakarta.validation.Valid
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

    data class Profile(val user: UserResponse)

    /** 낙네임 중복조회 nickname duplication check*/
    @GetMapping("/nickname/{nickname}")
    suspend fun nicknameDuplicationCheck(@PathVariable nickname: String): ResponseEntity<Boolean> =
        ResponseEntity.ok(userService.nicknameDuplicationCheck(nickname))

    /** 문자 인증번호 */

    /** 회원가입 */
    @PostMapping
    suspend fun register(@Valid userRegisterFrom: UserRegisterForm, @RequestPart("file", required = false) file: FilePart?): Profile =
        Profile(userService.register(userRegisterFrom, file) )

    /** 회원정보조회 */
    @GetMapping("/me")
    suspend fun me(@AuthenticationPrincipal(expression = "user")  user: UserEntity): Profile {
        return Profile(user.toResponse())
    }

    /** 회원정보수정 */
    @PostMapping("/me/update")
    suspend fun update(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Valid userUpdateForm: UserUpdateForm,
        @RequestPart("file") file: FilePart?
    ): Profile = Profile(userService.updateUser(user, userUpdateForm, file))




}