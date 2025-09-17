package kr.jiasoft.hiteen.feature.auth.infra

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class JwtAuthenticationManager(
    private val jwtProvider: JwtProvider,
    private val reactiveUserDetailsService: ReactiveUserDetailsService
) : ReactiveAuthenticationManager {

    private val log = LoggerFactory.getLogger(javaClass)

    class InvalidBearerToken(message: String?) : AuthenticationException(message)

    override fun authenticate(authentication: Authentication?): Mono<Authentication> {
        return Mono.justOrEmpty(authentication)
            .filter { it is BearerToken }
            .cast(BearerToken::class.java)
            .flatMap { token ->
                mono {
                    try {
                        validate(token)
                    } catch (e: ExpiredJwtException) {
                        log.info("JWT expired")
                        throw InvalidBearerToken("expired")
                    } catch (e: JwtException) {
                        log.warn("JWT invalid (reason={})", e.javaClass.simpleName)
                        throw InvalidBearerToken("invalid")
                    }
                }
            }
            // 불필요한 스택 노출 방지
            .onErrorMap(InvalidBearerToken::class.java) { it }
    }

    private suspend fun validate(token: BearerToken): Authentication {
        val jws = jwtProvider.parseAndValidateOrThrow(token)

        val username = jws.payload.subject

        // TODO Redis 캐시
        val userDetails = reactiveUserDetailsService.findByUsername(username).awaitFirstOrNull()
            ?: throw InvalidBearerToken("user_not_found")

        if (!jwtProvider.isValidWithUserMatches(token, userDetails)) {
            throw InvalidBearerToken("mismatch")
        }

        return UsernamePasswordAuthenticationToken(
            userDetails, userDetails.password, userDetails.authorities
        )
    }
}
