package kr.jiasoft.hiteen.feature.auth.app

import kotlinx.coroutines.reactor.mono
import kr.jiasoft.hiteen.feature.user.dto.CustomUserDetails
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class ReactiveUserDetailServiceImpl (
    private val userRepository: UserRepository,
) : ReactiveUserDetailsService {

    override fun findByUsername(username: String): Mono<UserDetails> = mono {
        val user = userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("User not found: $username")

        CustomUserDetails.from(user)
    }

}