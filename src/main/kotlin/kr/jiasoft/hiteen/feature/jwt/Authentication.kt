package kr.jiasoft.hiteen.feature.jwt

import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import kr.jiasoft.hiteen.feature.user.UserService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono


@Component
class JwtServerAuthenticationConverter : ServerAuthenticationConverter {

    override fun convert(exchange: ServerWebExchange): Mono<Authentication> {
        return Mono.justOrEmpty(exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION))
            .filter { it.startsWith("Bearer ") }
            .map { it.substring(7) }
            .map { jwt -> BearerToken(jwt) }
    }
}


class InvalidBearerToken(message: String?) : AuthenticationException(message)


@Component
class JwtAuthenticationManager(
    private val jwtProvider: JwtProvider,
    private val userService: UserService
) : ReactiveAuthenticationManager {

    private val log = LoggerFactory.getLogger(javaClass)


    override fun authenticate(authentication: Authentication?): Mono<Authentication> {
        return Mono.justOrEmpty(authentication)
            .filter { it is BearerToken }
            .cast(BearerToken::class.java)
            .flatMap { jwt ->
                mono {
                    try {
                        validate(jwt)
                    } catch (e: io.jsonwebtoken.ExpiredJwtException) {
//                        log.info("JWT expired (reason=expired, sub={})", jwtProvider.tryGetSubjectSafely(jwt))
                        log.info("JWT expired")
                        throw InvalidBearerToken("expired")
                    } catch (e: io.jsonwebtoken.JwtException) {
                        log.warn("JWT invalid (reason={})", e.javaClass.simpleName)
                        throw InvalidBearerToken("invalid")
                    }
                }
            }
            // 불필요한 원인 체인 제거(스택 노출 방지)
            .onErrorMap(InvalidBearerToken::class.java) { it }
    }


    private suspend fun validate(token: BearerToken): Authentication {
        val jws = jwtProvider.parseAndValidateOrThrow(token)

        val username = jws.payload.subject

        val userDetails = userService.findByUsername(username).awaitFirstOrNull()
            ?: throw InvalidBearerToken("user_not_found")

        if (!jwtProvider.isValidWithUserMatches(token, userDetails)) {
            throw InvalidBearerToken("mismatch")
        }
        return UsernamePasswordAuthenticationToken(
            userDetails, userDetails.password, userDetails.authorities
        )
    }

}
