package kr.jiasoft.hiteen.feature.auth.app

import kotlinx.coroutines.reactor.mono
import kr.jiasoft.hiteen.feature.user.app.UserService
import kr.jiasoft.hiteen.feature.user.dto.CustomUserDetails
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class ReactiveUserDetailServiceImpl (
    private val userRepository: UserRepository,
    private val userService: UserService,
) : ReactiveUserDetailsService {

//    @Cacheable(cacheNames = ["user"], key = "#username")
    override fun findByUsername(username: String): Mono<UserDetails> = mono {

//        val user = userRepository.findByUsername(username)
        val user = userService.findByUsernamee(username)
        CustomUserDetails.from(user!!)
    }

}