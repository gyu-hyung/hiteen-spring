package kr.jiasoft.hiteen.feature.user

import kotlinx.coroutines.reactive.awaitFirstOrNull
import kr.jiasoft.hiteen.feature.jwt.JwtProvider
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.security.Principal

@RestController
@RequestMapping("/api/user")
class UserController(
    private val userService: UserService,
    private val users: ReactiveUserDetailsService,
    private val encoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
) {

    data class Jwt(val token: String)
    data class Login(val username: String, val password: String)
    data class Profile(val username: String)



    @PostMapping("/login")
    suspend fun login(@RequestBody login: Login) : Jwt {
        val user = users.findByUsername(login.username).awaitFirstOrNull()
        println("user = $user")
        println("login.password = ${login.password}")
        println("user.password = ${user?.password}")
        println("encoder class = ${encoder::class.qualifiedName}")

        user?.let {
            println("encoder.matches = ${encoder.matches(login.password, it.password)}")
            if (encoder.matches(login.password, it.password)){
                println("login success")
                return Jwt(jwtProvider.generate(it.username).value)
            }
        }
        throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
    }


    @GetMapping("/me")
    suspend fun me(@AuthenticationPrincipal principal: Principal): Profile {
        return Profile(principal.name)
    }



}