package kr.jiasoft.hiteen.feature.auth.app

import kotlinx.coroutines.reactive.awaitFirstOrNull
import kr.jiasoft.hiteen.feature.auth.infra.JwtProvider
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val reactiveUserDetailsService: ReactiveUserDetailsService,
    private val encoder: PasswordEncoder,
    private val jwtProvider: JwtProvider
) {

    suspend fun login(username: String, rawPassword: String): String {
        val userDetails = reactiveUserDetailsService.findByUsername(username).awaitFirstOrNull()
            ?: throw IllegalArgumentException("Invalid credentials")
        if (!encoder.matches(rawPassword, userDetails.password)) {
            throw IllegalArgumentException("Invalid credentials")
        }
        return jwtProvider.generate(userDetails.username).value
    }

}
