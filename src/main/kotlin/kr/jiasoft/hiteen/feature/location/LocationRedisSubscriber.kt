package kr.jiasoft.hiteen.feature.location

import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class LocationRedisSubscriber(
    private val redisTemplate: ReactiveStringRedisTemplate
) {
    fun subscribeUserChannel(userId: String): Flux<String> {
        val channel = "loc:user:{$userId}"
        return redisTemplate.listenToChannel(channel).map { it.message }
    }
}