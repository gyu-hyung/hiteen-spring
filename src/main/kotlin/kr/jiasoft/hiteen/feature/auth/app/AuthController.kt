package kr.jiasoft.hiteen.feature.auth.app

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {
    data class LoginForm(val username: String, val password: String)
    data class JwtResponse(val token: String)


    @PostMapping("/login")
    suspend fun login(@RequestBody form: LoginForm): JwtResponse =
        try { JwtResponse(authService.login(form.username, form.password)) }
        catch (e: IllegalArgumentException) { throw ResponseStatusException(HttpStatus.UNAUTHORIZED) }


}
