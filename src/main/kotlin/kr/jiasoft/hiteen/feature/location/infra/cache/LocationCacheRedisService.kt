package kr.jiasoft.hiteen.feature.location.infra.cache

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kr.jiasoft.hiteen.feature.location.domain.LocationHistory
import kr.jiasoft.hiteen.feature.location.infra.db.LocationHistoryMongoRepository
import kr.jiasoft.hiteen.feature.soketi.domain.SoketiChannelPattern
import org.springframework.data.geo.Distance
import org.springframework.data.geo.Metrics
import org.springframework.data.geo.Point
import org.springframework.data.redis.connection.RedisGeoCommands
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.domain.geo.GeoReference
import org.springframework.data.redis.domain.geo.GeoShape
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class LocationCacheRedisService(
    private val redisTemplate: ReactiveStringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val locationHistoryMongoRepository: LocationHistoryMongoRepository
) {
    private val ttl: Duration = Duration.ofHours(24)

    /**
     * 최신 위치 캐시 저장 (JSON + GEO)
     */
    suspend fun cacheLatest(entity: LocationHistory) {
        val key = SoketiChannelPattern.PRIVATE_USER_LOCATION.format(entity.userId)
        val json = objectMapper.writeValueAsString(entity)

        // 1️⃣ 최신 위치 JSON 캐시
        redisTemplate.opsForValue().set(key, json).awaitFirstOrNull()
        redisTemplate.expire(key, ttl).awaitFirstOrNull()

        // 2️⃣ GEO 데이터 (모든 유저를 하나의 컬렉션에 저장)
        redisTemplate.opsForGeo().add(
            SoketiChannelPattern.PRIVATE_GEO.pattern,
            Point(entity.lng, entity.lat),
            "user:${entity.userId}"
        ).awaitFirstOrNull()
    }


    /**
     * 최신 위치 조회 (Redis → Mongo fallback)
     */
    suspend fun getLatest(userId: String): LocationHistory? {
        val key = SoketiChannelPattern.PRIVATE_USER_LOCATION.format(userId)
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


    /**
     * 거리 기반 근처 사용자 조회
     * @param userId 기준 사용자 ID
     * @param radiusKm 검색 반경 (단위: km)
     * @return GEO 내 반경 사용자 userId Set
     */
    suspend fun findNearbyUserIds(userId: String, radiusKm: Double = 5.0): Set<Long> {
        val myLocation = getLatest(userId) ?: return emptySet()
        val myPoint = Point(myLocation.lng, myLocation.lat)
        val distance = Distance(radiusKm, Metrics.KILOMETERS)

        val reference = GeoReference.fromCoordinate<String>(myPoint)
        val shape = GeoShape.byRadius(distance)

        val args = RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
            .includeCoordinates()
            .sortAscending()

        val results = redisTemplate.opsForGeo()
            .search(SoketiChannelPattern.PRIVATE_GEO.pattern, reference, shape, args)
            .collectList().awaitSingleOrNull() ?: emptyList()

        return results.mapNotNull { it.content?.name }
            .mapNotNull { name -> name.removePrefix("user:").toLongOrNull() }
            .filterNot { it.toString() == userId }
            .toSet()
    }


    suspend fun testConnection(): String {
        val result = redisTemplate.execute { it.ping() }.awaitFirstOrNull()
        return result ?: "NO RESPONSE"
    }



}
