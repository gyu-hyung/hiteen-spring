package kr.jiasoft.hiteen.feature.auth.app

import kr.jiasoft.hiteen.feature.auth.dto.JwtResponse
import kr.jiasoft.hiteen.feature.auth.infra.JwtProvider
import kr.jiasoft.hiteen.feature.user.app.UserService
import kr.jiasoft.hiteen.feature.user.dto.UserResponseWithTokens
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val encoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
    private val userService: UserService,
    private val userRepository: UserRepository,
) {

    suspend fun login(username: String, rawPassword: String): UserResponseWithTokens {
        val userEntity = userRepository.findActiveByUsername(username)
            ?: throw IllegalArgumentException("유효하지 않은 사용자야~")
        if (!encoder.matches(rawPassword, userEntity.password)) {
//            throw IllegalArgumentException("Invalid credentials")
            throw IllegalArgumentException("유효하지 않은 사용자야~")
        }
        val userResponse = userService.findUserResponse(userEntity.username)

        val (access, refresh) = jwtProvider.generateTokens(userEntity.username)

        return UserResponseWithTokens(
            userResponse = userResponse,
            tokens = JwtResponse(access.value, refresh.value)
        )

    }


}
