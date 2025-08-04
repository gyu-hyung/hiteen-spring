package kr.jiasoft.hiteen.feature.user

import kotlinx.coroutines.reactor.mono
import kr.jiasoft.hiteen.feature.user.dto.UserRegisterForm
import kr.jiasoft.hiteen.feature.user.dto.UserResponse
import kr.jiasoft.hiteen.feature.user.dto.UserUpdateForm
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Service
class UserService (
    private val userRepository: UserRepository,
    private val encoder: PasswordEncoder
) : ReactiveUserDetailsService {

    override fun findByUsername(username: String): Mono<UserDetails> = mono {
        val user = userRepository.findByUsername(username)
        user?.toUserDetails() ?: throw UsernameNotFoundException("User not found: $username")
    }

    suspend fun register(param: UserRegisterForm): UserResponse {
        val entity = param.toEntity(encoder.encode(param.password))
        return userRepository.save(entity).toResponse()
    }

    //TODO 회원 정보 변경 시 로그아웃 처리 어떻게 함? 1.JWT disable 2.?
    suspend fun updateUser(user: UserEntity, param: UserUpdateForm): UserResponse {
        val user = userRepository.findById(user.id!!)
            ?: throw UsernameNotFoundException("User not found: ${user.username}")
        val updated = user.copy(
            nickname = param.nickname ?: user.nickname,
            email = param.email ?: user.email,
            password = param.password?.let { encoder.encode(it) } ?: user.password,
            updatedAt = LocalDateTime.now(),
        )
        return userRepository.save(updated).toResponse()
    }



}