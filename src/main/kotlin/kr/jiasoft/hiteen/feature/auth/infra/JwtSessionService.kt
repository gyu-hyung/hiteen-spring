package kr.jiasoft.hiteen.feature.auth.infra

import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * JWT 세션 관리 서비스
 * - 사용자별로 현재 유효한 토큰을 Redis에 저장
 * - 새 로그인 시 기존 토큰 무효화 (중복 로그인 방지)
 */
@Service
class JwtSessionService(
    private val redisTemplate: ReactiveStringRedisTemplate,
) {
    companion object {
        private const val SESSION_KEY_PREFIX = "jwt:session:"
        private val SESSION_TTL = Duration.ofDays(7) // refresh token TTL과 동일하게
    }

    /**
     * 새 세션 등록 (기존 세션 무효화)
     * @param username 사용자 식별자
     * @param tokenId 토큰 식별자 (jti 또는 토큰 해시)
     */
    suspend fun registerSession(username: String, tokenId: String) {
        val key = SESSION_KEY_PREFIX + username
        redisTemplate.opsForValue()
            .set(key, tokenId, SESSION_TTL)
            .awaitFirstOrNull()
    }

    /**
     * 현재 세션이 유효한지 확인
     * @param username 사용자 식별자
     * @param tokenId 토큰 식별자
     * @return 유효하면 true
     */
    suspend fun isValidSession(username: String, tokenId: String): Boolean {
        val key = SESSION_KEY_PREFIX + username
        val storedTokenId = redisTemplate.opsForValue()
            .get(key)
            .awaitFirstOrNull()
        return storedTokenId == tokenId
    }

    /**
     * 세션이 존재하는지 확인 (Redis 데이터 유실 대비)
     * @param username 사용자 식별자
     * @return 세션이 있으면 true
     */
    suspend fun hasSession(username: String): Boolean {
        val key = SESSION_KEY_PREFIX + username
        val exists = redisTemplate.hasKey(key).awaitFirstOrNull()
        return exists == true
    }

    /**
     * 세션 무효화 (로그아웃)
     * @param username 사용자 식별자
     */
    suspend fun invalidateSession(username: String) {
        val key = SESSION_KEY_PREFIX + username
        redisTemplate.delete(key).awaitFirstOrNull()
    }
}

