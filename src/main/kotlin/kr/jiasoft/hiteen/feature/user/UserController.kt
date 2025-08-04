package kr.jiasoft.hiteen.feature.user

import jakarta.validation.Valid
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kr.jiasoft.hiteen.feature.jwt.JwtProvider
import kr.jiasoft.hiteen.feature.user.dto.UserRegisterForm
import kr.jiasoft.hiteen.feature.user.dto.UserResponse
import kr.jiasoft.hiteen.feature.user.dto.UserUpdateForm
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/user")
class UserController(
    private val userService: UserService,
    private val encoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
) {

    data class Jwt(val token: String)
    data class LoginForm(val username: String, val password: String)
    data class Profile(val user: UserResponse)


    /* 회원가입 */
    @PostMapping
    suspend fun register(@Valid userRegisterFrom: UserRegisterForm): Profile =
        Profile(userService.register(userRegisterFrom))

    /* 로그인 */
    @PostMapping("/login")
    suspend fun login(loginForm: LoginForm) : Jwt {
        val user = userService.findByUsername(loginForm.username).awaitFirstOrNull()
        user?.let {
            if (encoder.matches(loginForm.password, it.password)){
                println("login success")
                return Jwt(jwtProvider.generate(it.username).value)
            }
        }
        throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
    }

    /* 회원정보조회 */
    @GetMapping("/me")
    suspend fun me(@AuthenticationPrincipal(expression = "user")  user: UserEntity): Profile {
        println("principal = ${user}")
        return Profile(user.toResponse())
    }

    /* 회원정보수정 */
    @PostMapping("/me/update")
    suspend fun update(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Valid userUpdateForm: UserUpdateForm
    ): Profile = Profile(userService.updateUser(user, userUpdateForm))




}