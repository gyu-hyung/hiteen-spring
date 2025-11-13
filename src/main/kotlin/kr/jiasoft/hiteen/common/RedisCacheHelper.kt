package kr.jiasoft.hiteen.common

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.stereotype.Component

@Component
class RedisCacheHelper(
    val redis: ReactiveRedisTemplate<String, String>,
    objectMapper: ObjectMapper
) {

    // JSON Serializer (타입 정보 포함)
    val serializer = GenericJackson2JsonRedisSerializer(objectMapper)

    /**
     * Redis 캐시 헬퍼
     * - 캐시에 값이 있으면 JSON → T 로 역직렬화하여 반환
     * - 없으면 block() 실행 후 Redis 에 저장
     */
    final suspend inline fun <reified T : Any> getOrPut(
        key: String,
        noinline block: suspend () -> T?
    ): T? {
        // 1) 캐시 조회
        val cachedJson = redis.opsForValue().get(key).awaitFirstOrNull()
        if (cachedJson != null) {
            return serializer.deserialize(cachedJson.toByteArray(), T::class.java)
        }

        // 2) 계산 or DB 조회
        val value = block() ?: return null

        // 3) Redis 저장
        val storedJson = serializer.serialize(value)
        redis.opsForValue().set(key, String(storedJson!!)).awaitFirstOrNull()

        return value
    }
}
