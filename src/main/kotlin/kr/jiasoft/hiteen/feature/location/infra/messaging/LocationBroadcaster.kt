package kr.jiasoft.hiteen.feature.location.infra.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kr.jiasoft.hiteen.feature.location.domain.LocationHistory
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.stereotype.Service

data class LocationEvent(
    val userId: String,
    val lat: Double,
    val lng: Double,
    val timestamp: Long,
    val source: String = "http"
) {
    companion object {
        fun from(h: LocationHistory) =
            LocationEvent(h.userId, h.lat, h.lng, h.timestamp, "http")
    }
}

//실시간 브로드캐스팅
@Service
class LocationBroadcaster(
    private val redisTemplate: ReactiveStringRedisTemplate,
    private val objectMapper: ObjectMapper
) {
    fun channelForUser(userId: String) = "loc:user:{$userId}"

    suspend fun publishToUser(userId: String, event: LocationEvent) {
        val payload = objectMapper.writeValueAsString(event)
        redisTemplate.convertAndSend(channelForUser(userId), payload).awaitFirstOrNull()
    }
}