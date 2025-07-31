package kr.jiasoft.hiteen.feature.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import jakarta.annotation.PostConstruct
import kr.jiasoft.hiteen.feature.user.UserEntity
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtProvider(
    @Value("\${jwt.secret}")
    private val secret: String,
    @Value("\${jwt.access-expiration}")
    private val accessExpiration: Long,
    @Value("\${jwt.refresh-expiration}")
    private val refreshExpiration: Long,
) {

    private lateinit var key: SecretKey

    @PostConstruct
    fun init() {
        key = Keys.hmacShaKeyFor(secret.toByteArray())
    }

    /** 액세스 토큰 생성 */
    fun createAccessToken(claims: Map<String, Any?>): String =
        createToken(claims, accessExpiration)

    /** 액세스 토큰 생성 */
    fun createRefreshToken(claims: Map<String, Any?>): String =
        createToken(claims, refreshExpiration)

    /** 토큰 생성 공통 */
    private fun createToken(claims: Map<String, Any?>, expiration: Long): String {
        val now = System.currentTimeMillis()
        return Jwts.builder()
            .claims(claims)
            .issuedAt(Date(now))
            .expiration(Date(now + expiration))
            .signWith(key)
            .compact()
    }

    /** Claims 추출 */
    fun parseClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload



    /** Account-like 객체로 파싱 예시 */
    fun parseAccount(token: String): UserEntity =
        parseClaims(token).let { claims ->
            UserEntity(
                id = claims["id"]?.toString()?.toLongOrNull(),
                name = claims["name"]?.toString(),
//                email = claims["email"]?.toString() ?: claims.subject,
//                provider = claims["provider"]?.toString(),
//                profileImage = claims["profileImage"]?.toString(),
                role = claims["role"]?.toString() ?: "USER"
            )
        }




    /** 토큰 유효성 검사 (만료/구조/시그니처 등) */
    fun validateToken(token: String): Boolean = try {
        parseClaims(token)
        true
    } catch (e: Exception) {
        false
    }

    /** 이메일(subject) 추출 */
    fun getEmail(token: String): String? =
        parseClaims(token).subject
}

