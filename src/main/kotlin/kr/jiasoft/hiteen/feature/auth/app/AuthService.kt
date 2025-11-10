package kr.jiasoft.hiteen.feature.auth.app

import kr.jiasoft.hiteen.feature.auth.dto.JwtResponse
import kr.jiasoft.hiteen.feature.auth.infra.JwtProvider
import kr.jiasoft.hiteen.feature.user.app.UserService
import kr.jiasoft.hiteen.feature.user.dto.UserResponseWithTokens
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class AuthService(
    private val encoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
    private val userService: UserService,
    private val userRepository: UserRepository,
) {

    suspend fun login(username: String, rawPassword: String): UserResponseWithTokens {
        val userDetails = userRepository.findActiveByUsername(username)
            ?: throw IllegalArgumentException("Invalid credentials")
        if (!encoder.matches(rawPassword, userDetails.password)) {
//            throw IllegalArgumentException("Invalid credentials")
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid credentials")
        }
        val userResponse = userService.findUserResponse(userDetails.username)

        val (access, refresh) = jwtProvider.generateTokens(userDetails.username)



        return UserResponseWithTokens(
            userResponse = userResponse,
            tokens = JwtResponse(access.value, refresh.value)
        )

    }


}
