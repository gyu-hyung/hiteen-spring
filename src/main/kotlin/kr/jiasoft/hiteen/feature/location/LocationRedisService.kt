package kr.jiasoft.hiteen.feature.location

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class LocationRedisService(
    private val redisTemplate: ReactiveStringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val locationHistoryMongoRepository: LocationHistoryMongoRepository
) {
    // 캐시 키 (해시태그로 동일 슬롯 보장)
    fun latestKey(userId: String) = "loc:latest:user:{$userId}"
    // 선택: 캐시 TTL
    private val ttl: Duration = Duration.ofHours(24)


    //최신 위치 갱신
    suspend fun cacheLatest(entity: LocationHistory) {
        val json = objectMapper.writeValueAsString(entity)
        // TTL을 쓰고 싶지 않다면 expire 부분 제거
        redisTemplate.opsForValue().set(latestKey(entity.userId), json).awaitFirstOrNull()
        redisTemplate.expire(latestKey(entity.userId), ttl).awaitFirstOrNull()
    }


    suspend fun getLatest(userId: String): LocationHistory? {
        // 1) Redis 캐시 조회
        val json = redisTemplate.opsForValue().get(latestKey(userId)).awaitFirstOrNull()
        if (!json.isNullOrBlank()) {
            return objectMapper.readValue(json, LocationHistory::class.java)
        }
        // 2) 폴백: Mongo 최신 1건
        val fromDb = locationHistoryMongoRepository
            .findTopByUserIdOrderByTimestampDesc(userId)
            .awaitFirstOrNull()

        // 3) 폴백 결과를 캐시에 되적재(있다면)
        if (fromDb != null) {
            cacheLatest(fromDb)
        }
        return fromDb
    }
}
