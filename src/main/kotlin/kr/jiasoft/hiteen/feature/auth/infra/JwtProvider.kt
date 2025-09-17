package kr.jiasoft.hiteen.feature.auth.infra

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


    /** 토큰을 완전 검증(서명/exp/nbf)하고 Claims를 반환. 실패 시 JwtException 계열을 던짐 */
    fun parseAndValidateOrThrow(token: BearerToken): Jws<Claims> {
        return parser.parseSignedClaims(token.value)
    }

    /** username으로 Access/Refresh 동시 발급 */
    fun generateTokens(username: String): Pair<BearerToken, BearerToken> {
        val now = Date()

        val access = Jwts.builder()
            .subject(username)
            .issuedAt(now)
            .expiration(Date(now.time + accessExpiration))
            .signWith(key)
            .compact()

        val refresh = Jwts.builder()
            .subject(username)
            .issuedAt(now)
            .expiration(Date(now.time + refreshExpiration))
            .signWith(key)
            .compact()

        return BearerToken(access) to BearerToken(refresh)
    }


    fun refreshTokens(refreshToken: BearerToken): Pair<BearerToken, BearerToken> {
        val claims = parser.parseSignedClaims(refreshToken.value).payload

        if (claims.expiration.before(Date())) {
            throw JwtException("Refresh expired")
        }

        val username = claims.subject

        // 새 Access & Refresh 발급 (자동 연장)
        return generateTokens(username)
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
//            e.printStackTrace()
            false
        }
    }


}