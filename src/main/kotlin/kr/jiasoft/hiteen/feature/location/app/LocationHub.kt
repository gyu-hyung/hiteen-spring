package kr.jiasoft.hiteen.feature.location.app

import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.util.concurrent.ConcurrentHashMap

/**
 * 위치 실시간 구독 허브 (발행은 HTTP에서 수행).
 * 채널 규약: loc:user:{userUid}
 */
@Component
class LocationHub(
    private val container: ReactiveRedisMessageListenerContainer
) {
    private val localBridges = ConcurrentHashMap<String, Flux<String>>()

    /** 단일 유저 구독 */
    fun subscribeUser(userUid: String): Flux<String> =
        localBridges.computeIfAbsent(userUid) { uid ->
            val topic = ChannelTopic(userTopic(uid))
            container.receive(topic)          // Redis Pub/Sub 구독
                .map { it.message }           // payload(String, JSON 가정)
                .share()                      // 동일 userUid 다중 세션 공유
        }

    /** 여러 유저 병합 구독 */
    fun subscribeUsers(userUids: List<String>): Flux<String> =
        Flux.merge(userUids.map { subscribeUser(it) })

    private fun userTopic(userUid: String) = "loc:user:$userUid"
}
