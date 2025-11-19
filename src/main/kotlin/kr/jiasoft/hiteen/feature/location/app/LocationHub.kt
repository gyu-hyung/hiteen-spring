package kr.jiasoft.hiteen.feature.location.app

import kr.jiasoft.hiteen.config.websocket.RedisChannelPattern
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 위치 실시간 구독 허브 (발행은 HTTP에서 수행).
 * 채널 규약: loc:user:{userUid}
 */
@Component
class LocationHub(
    private val publisher: ReactiveStringRedisTemplate,
    private val subscriber: ReactiveRedisMessageListenerContainer
) {
    private val localBridges = ConcurrentHashMap<String, Flux<String>>()

    /** 단일 유저 구독 */
    fun subscribeUser(userUid: UUID): Flux<String> =
        localBridges.computeIfAbsent(userUid.toString()) { uid ->
            val topic = ChannelTopic(userTopic(UUID.fromString(uid)))
            subscriber.receive(topic)          // Redis Pub/Sub 구독
                .map { it.message }           // payload(String, JSON 가정)
                .share()                      // 동일 userUid 다중 세션 공유
        }

    /** 여러 유저 병합 구독 */
    fun subscribeUsers(userUids: List<UUID>): Flux<String> =
        Flux.merge(userUids.map { subscribeUser(it) })


    fun publish(userUid: UUID, json: String) {
        publisher.convertAndSend(userTopic(userUid), json).subscribe()
    }

    private fun userTopic(userUid: UUID) = RedisChannelPattern.USER_LOCATION.format(userUid)
}
