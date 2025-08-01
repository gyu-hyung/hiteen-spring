package kr.jiasoft.hiteen.feature.user

import kotlinx.coroutines.reactor.mono
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class UserService (
    private val userRepository: UserRepository,
) : ReactiveUserDetailsService{

    override fun findByUsername(username: String): Mono<UserDetails> = mono{
        val user = userRepository.findByUsername(username)
        user?.toUserDetails() ?: throw UsernameNotFoundException("User not found: $username")
    }

    suspend fun getUserById(id: Long): UserEntity? =
        userRepository.findById(id)

    suspend fun createUser(user: UserEntity): UserEntity =
        userRepository.save(user)

}