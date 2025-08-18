package kr.jiasoft.hiteen.feature.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey


class BearerToken(val value: String) : AbstractAuthenticationToken(AuthorityUtils.NO_AUTHORITIES) {
    override fun getCredentials(): Any = value
    override fun getPrincipal(): Any = value
}

@Component
class JwtProvider (
    @Value("\${jwt.secret}")
    private val secret: String,
    @Value("\${jwt.access-expiration}")
    private val accessExpiration: Long,
    @Value("\${jwt.refresh-expiration}")
    private val refreshExpiration: Long,
) {

    private lateinit var key: SecretKey
    private lateinit var parser: JwtParser

    @PostConstruct
    fun init() {
        key = Keys.hmacShaKeyFor(secret.toByteArray())
        parser = Jwts.parser().verifyWith(key).build()
    }



    /** username으로 BearerToken(AccessToken) 발급 */
    fun generate(username: String): BearerToken {
        val now = Date()
        val token = Jwts.builder()
            .subject(username)
            .issuedAt(now)
            .expiration(Date(now.time + accessExpiration))
            .signWith(key)
            .compact()
        return BearerToken(token)
    }

    fun generateExpiredToken(): BearerToken {
        val now = Date()
        val token = Jwts.builder()
            .subject("test")
            .issuedAt(now)
            .expiration(Date(now.time - accessExpiration))
            .signWith(key)
            .compact()
        return BearerToken(token)
    }

    /** BearerToken에서 username(subject) 추출 */
    fun getUsername(token: BearerToken): String {
        return try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token.value)
                .payload
                .subject
        } catch (e: Exception) {
//            e.printStackTrace()
            ""
        }
    }


    /** 토큰을 완전 검증(서명/exp/nbf)하고 Claims를 반환. 실패 시 JwtException 계열을 던짐 */
    fun parseAndValidateOrThrow(token: BearerToken): Jws<Claims> {
        return parser.parseSignedClaims(token.value)
    }

    /** 로깅 전용: 안전하게 subject를 꺼내되 실패하면 null */
    fun tryGetSubjectSafely(token: BearerToken): String? =
        try { parser.parseSignedClaims(token.value).payload.subject }
        catch (_: JwtException) { null }
        catch (_: IllegalArgumentException) { null }


    /** BearerToken 유효성 검사 및 사용자 체크 */
    fun isValid(token: BearerToken): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token.value)
                .payload
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    /** BearerToken 유효성 검사 및 사용자 체크 */
    fun isValidWithUserMatches(token: BearerToken, user: UserDetails?): Boolean {
        return try {
            val claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token.value)
                .payload
            // 토큰 만료 확인 및 username(UserDetails) 일치 확인
            val expired = claims.expiration.before(Date())
            val matches = user?.username == claims.subject
            !expired && matches
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


}