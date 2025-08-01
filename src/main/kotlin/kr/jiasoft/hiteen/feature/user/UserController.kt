package kr.jiasoft.hiteen.feature.user

import kotlinx.coroutines.reactive.awaitFirstOrNull
import kr.jiasoft.hiteen.feature.jwt.JwtProvider
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
    data class Login(val username: String, val password: String)
    data class Profile(val user: UserEntity)



    @PostMapping("/login")
    suspend fun login(login: Login) : Jwt {
        val user = userService.findByUsername(login.username).awaitFirstOrNull()
        user?.let {
            if (encoder.matches(login.password, it.password)){
                println("login success")
                return Jwt(jwtProvider.generate(it.username).value)
            }
        }
        throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
    }


    @GetMapping("/me")
    suspend fun me(@AuthenticationPrincipal(expression = "user")  user: UserEntity): Profile {
        println("principal = ${user}")
        return Profile(user)
    }



}