package kr.jiasoft.hiteen.feature.location.infra.cache

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kr.jiasoft.hiteen.feature.location.domain.LocationHistory
import kr.jiasoft.hiteen.feature.location.infra.db.LocationHistoryMongoRepository
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class LocationCacheRedisService(
    private val redisTemplate: ReactiveStringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val locationHistoryMongoRepository: LocationHistoryMongoRepository
) {
    private val ttl: Duration = Duration.ofHours(24)

    fun latestKey(userId: String) = "loc:latest:user:$userId"

    suspend fun cacheLatest(entity: LocationHistory) {
        val key = latestKey(entity.userId)
        val json = objectMapper.writeValueAsString(entity)
        redisTemplate.opsForValue().set(key, json).awaitFirstOrNull()
        redisTemplate.expire(key, ttl).awaitFirstOrNull()
    }

    suspend fun getLatest(userId: String): LocationHistory? {
        val key = latestKey(userId)
        val cached = redisTemplate.opsForValue().get(key).awaitFirstOrNull()
        if (!cached.isNullOrBlank()) {
            return objectMapper.readValue(cached, LocationHistory::class.java)
        }
        val fromDb = locationHistoryMongoRepository
            .findTopByUserIdOrderByTimestampDesc(userId)
            .awaitFirstOrNull()
        if (fromDb != null) cacheLatest(fromDb)
        return fromDb
    }
}
