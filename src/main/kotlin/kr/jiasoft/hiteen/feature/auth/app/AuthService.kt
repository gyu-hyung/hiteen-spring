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
            ?: throw IllegalArgumentException("""
                아직 하이틴의 친구가 아니야
                지금 당장 회원가입부터 하자!
            """.trimIndent())
        if (!encoder.matches(rawPassword, userEntity.password)) {
//            throw IllegalArgumentException("Invalid credentials")
            throw IllegalArgumentException("""
                비밀번호가 맞지 않아.
                다시 한번 확인 해줘
            """.trimIndent())
        }
        val userResponse = userService.findUserResponse(userEntity.username)

        val (access, refresh) = jwtProvider.generateTokens(userEntity.username)

        return UserResponseWithTokens(
            userResponse = userResponse,
            tokens = JwtResponse(access.value, refresh.value)
        )

    }


}
