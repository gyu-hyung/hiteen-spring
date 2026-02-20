package kr.jiasoft.hiteen.feature.auth.infra

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import kr.jiasoft.hiteen.feature.auth.app.AuthLogService
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.util.Date

@Component
class JwtAuthenticationManager(
    private val jwtProvider: JwtProvider,
    private val jwtSessionService: JwtSessionService,
    private val reactiveUserDetailsService: ReactiveUserDetailsService,
    private val authLogService: AuthLogService
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
                        authLogService.saveLog(null, "EXPIRED", token, "JWT expired")
                        throw InvalidBearerToken("expired")
                    } catch (e: JwtException) {
                        log.warn("JWT invalid (reason={})", e.javaClass.simpleName)
                        authLogService.saveLog(null, "INVALID_JWT", token, "JWT invalid: ${e.javaClass.simpleName}")
                        throw InvalidBearerToken("invalid")
                    } catch (e: InvalidBearerToken) {
                        throw e
                    } catch (e: Exception) {
                        log.error("Auth error", e)
                        authLogService.saveLog(null, "ERROR", token, "System error: ${e.message}")
                        throw InvalidBearerToken("error")
                    }
                }
            }
            // 불필요한 스택 노출 방지
            .onErrorMap(InvalidBearerToken::class.java) { it }
    }

    private suspend fun validate(token: BearerToken): Authentication {
        val jws = jwtProvider.parseAndValidateOrThrow(token)

        val username = jws.payload.subject
        val jti = jws.payload.id

        // Special rule: if username == 01095393637, treat access tokens older than 1 minute as expired
        if (username == "01095393637") {
            val issuedAt: Date? = jws.payload.issuedAt
            if (issuedAt != null) {
                val ageMs = Date().time - issuedAt.time
                if (ageMs > 60_000L) {
                    log.info("Token too old for special user={}, ageMs={}", username, ageMs)
                    authLogService.saveLog(username, "EXPIRED_POLICY", token, "Token older than 1 minute")
                    throw InvalidBearerToken("expired")
                }
            }
        }

        // 🔒 Redis 세션 검증 (중복 로그인 방지)
        // - jti가 있는 토큰만 검증
        // - Redis에 세션이 없으면 허용 (Redis 데이터 유실 대비)
        if (jti != null) {
            val isValid = jwtSessionService.isValidSession(username, jti)
            val hasSession = jwtSessionService.hasSession(username)

            // Redis에 세션이 있고, jti가 일치하지 않으면 거부
            if (hasSession && !isValid) {
                log.info("Session invalid for user={}, jti={}", username, jti)
                authLogService.saveLog(username, "SESSION_INVALID", token, "Requested jti=$jti")
                throw InvalidBearerToken("session_invalid")
            }
        }

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
