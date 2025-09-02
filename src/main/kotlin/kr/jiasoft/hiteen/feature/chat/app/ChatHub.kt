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

    // 로컬 브리지: 같은 인스턴스 내 여러 세션이 같은 room을 구독할 때 Redis 수신 스트림을 1개로 공유(멀티캐스트)해서 중복 수신/부하를 줄임.
    private val localBridges = ConcurrentHashMap<UUID, Flux<String>>()

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

    fun publish(roomUid: UUID, json: String) {
        publisher.convertAndSend(roomTopic(roomUid), json).subscribe()
    }

    /** 접속자 관리 (분산) */
    fun join(roomUid: UUID, userId: Long, userUid: UUID) {
        publishSystem(roomUid, "join", userUid)
        publisher.opsForSet().add(roomMembersKey(roomUid), userId.toString()).subscribe()
        // presence TTL (선택): 60초마다 heartbeat를 갱신하게 하면 온라인 표시 가능
//        redis.opsForValue().set(presenceKey(roomUid, userId), "1", Duration.ofSeconds(60)).subscribe()
    }

    fun leave(roomUid: UUID, userId: Long, userUid: UUID) {
        publishSystem(roomUid, "leave", userUid)
        publisher.opsForSet().remove(roomMembersKey(roomUid), userId.toString()).subscribe()
    }

    /** 분산 멤버 카운트 반환: 동기 Int 대신 Mono<Int>를 권장 */
    fun memberCountMono(roomUid: UUID): Mono<Int> =
        publisher.opsForSet().size(roomMembersKey(roomUid)).map { it?.toInt() ?: 0 }.defaultIfEmpty(0)

    private fun publishSystem(roomUid: UUID, event: String, userUid: UUID) {
        val json = """{"type":"system","event":"$event","userUid":"$userUid","at":"${OffsetDateTime.now()}"}"""
        publish(roomUid, json)
    }

    private fun roomTopic(roomUid: UUID) = "chat:room:$roomUid"
    private fun roomMembersKey(roomUid: UUID) = "chat:room:$roomUid:members"
    private fun presenceKey(roomUid: UUID, userId: Long) = "chat:room:$roomUid:presence:$userId"
}
