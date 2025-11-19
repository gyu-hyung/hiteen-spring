package kr.jiasoft.hiteen.feature.chat.app

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class ChatHub(
    private val publisher: ReactiveStringRedisTemplate,
    private val subscriber: ReactiveRedisMessageListenerContainer,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // 방 단위 Redis pubsub → 로컬 브리지
    private val localBridges = ConcurrentHashMap<UUID, Flux<String>>()

    // 유저 단위 notify 채널 → 로컬 브리지
    private val userNotifyBridges = ConcurrentHashMap<UUID, Flux<String>>()

    /** 채팅방(실제 메시지) 구독 */
    fun subscribe(roomUid: UUID): Flux<String> =
        localBridges.computeIfAbsent(roomUid) { uid ->
            val topic = ChannelTopic(roomTopic(uid))
            subscriber.receive(topic)
                .map { it.message }
                .doOnSubscribe { log.debug("chat subscribe roomUid={}", uid) }
                .doFinally { signal ->
                    log.debug("chat unsubscribe roomUid={} signal={}", uid, signal)
                    localBridges.remove(uid)
                }
                .share()
        }

    /** 유저별 채팅방 목록 notify 구독 */
    fun subscribeUserNotify(userUid: UUID): Flux<String> =
        userNotifyBridges.computeIfAbsent(userUid) { uid ->
            val topic = ChannelTopic(userNotifyTopic(uid))
            subscriber.receive(topic)
                .map { it.message }
                .doOnSubscribe { log.debug("notify subscribe userId={}", uid) }
                .doFinally { signal ->
                    log.debug("notify unsubscribe userId={} signal={}", uid, signal)
                    userNotifyBridges.remove(uid)
                }
                .share()
        }

    /** 방 메시지 publish */
    fun publish(roomUid: UUID, json: String) {
        publisher.convertAndSend(roomTopic(roomUid), json).subscribe()
    }

    /** 유저 notify 메시지 publish (채팅방 목록용) */
    fun publishUserNotify(userUid: UUID, json: String) {
        publisher.convertAndSend(userNotifyTopic(userUid), json).subscribe()
    }

    /** 접속자 관리 (분산) */
    fun join(roomUid: UUID, userId: Long, userUid: UUID) {
        publishSystem(roomUid, "join", userUid)
        publisher.opsForSet().add(roomMembersKey(roomUid), userId.toString()).subscribe()
    }

    fun leave(roomUid: UUID, userId: Long, userUid: UUID) {
        publishSystem(roomUid, "leave", userUid)
        publisher.opsForSet().remove(roomMembersKey(roomUid), userId.toString()).subscribe()
    }

    /** 분산 멤버 카운트 반환 */
    fun memberCountMono(roomUid: UUID): Mono<Int> =
        publisher.opsForSet().size(roomMembersKey(roomUid))
            .map { it?.toInt() ?: 0 }
            .defaultIfEmpty(0)

    private fun publishSystem(roomUid: UUID, event: String, userUid: UUID) {
        val json =
            """{"type":"system","data":{ "event":"$event", "userUid":"$userUid", "at":"${OffsetDateTime.now()}" }}""".trimIndent()
        publish(roomUid, json)
    }

    private fun roomTopic(roomUid: UUID) = "chat:room:$roomUid"

    private fun roomMembersKey(roomUid: UUID) = "chat:room:$roomUid:members"

    private fun presenceKey(roomUid: UUID, userId: Long) = "chat:room:$roomUid:presence:$userId"

    private fun userNotifyTopic(userUid: UUID) = "user:$userUid:notify"
}
