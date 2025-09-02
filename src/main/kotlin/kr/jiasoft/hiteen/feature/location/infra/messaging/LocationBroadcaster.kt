package kr.jiasoft.hiteen.feature.location.infra.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kr.jiasoft.hiteen.feature.location.dto.LocationEvent
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.stereotype.Service

@Service
class LocationBroadcaster(
    private val redisTemplate: ReactiveStringRedisTemplate,
    private val objectMapper: ObjectMapper
) {
    fun channelForUser(userId: String) = "loc:user:$userId"

    //SUBSCRIBE "loc:user:<userUid>"
    suspend fun publishToUser(userUid: String, event: LocationEvent) {
        val payload = objectMapper.writeValueAsString(event)
        redisTemplate.convertAndSend(channelForUser(userUid), payload).awaitFirstOrNull()
    }
}