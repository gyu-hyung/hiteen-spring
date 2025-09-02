package kr.jiasoft.hiteen.feature.chat.app

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 퍼-유저 개인 큐(인박스)용 허브.
 * 여러 인스턴스 환경에서 Redis Pub/Sub 채널을 통해 fan-out.
 *
 * 채널 키: chat:inbox:{userId}
 */
@Component
class InboxHub(
    private val redis: ReactiveStringRedisTemplate,
    private val container: ReactiveRedisMessageListenerContainer
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val localBridges = ConcurrentHashMap<UUID, Flux<String>>()

    fun subscribe(userUid: UUID): Flux<String> =
        localBridges.computeIfAbsent(userUid) { uid ->
            val topic = ChannelTopic(inboxTopic(uid))
            container.receive(topic)
                .map { it.message }
                .doOnSubscribe { log.debug("inbox subscribe userUid={}", uid) }
                .doFinally { signal ->
                    log.debug("inbox unsubscribed userUid={} signal={}", uid, signal)
                    localBridges.remove(uid)
                }
                .share()
        }

    fun publishTo(userUid: UUID, json: String) {
        redis.convertAndSend(inboxTopic(userUid), json)
            .doOnError { e -> log.error("inbox publish error userUid={}", userUid, e) }
            .subscribe()
    }

    private fun inboxTopic(userUid: UUID) = "chat:inbox:$userUid"
}