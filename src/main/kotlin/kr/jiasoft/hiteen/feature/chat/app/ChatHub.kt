package kr.jiasoft.hiteen.feature.chat.app

import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class ChatHub {

    private data class RoomChannel(
        val sink: Sinks.Many<String>,
        val members: MutableSet<Long> = ConcurrentHashMap.newKeySet()
    )

    private val rooms = ConcurrentHashMap<UUID, RoomChannel>()

    fun subscribe(roomUid: UUID): Flux<String> =
        channel(roomUid).sink.asFlux()

    fun publish(roomUid: UUID, json: String) {
        channel(roomUid).sink.tryEmitNext(json)
    }

    /** 접속자 수 관리 (옵션) */
    fun join(roomUid: UUID, userId: Long, userUid: UUID) {
        channel(roomUid).members.add(userId)
        publishSystem(roomUid, "join", userUid)
    }

    fun leave(roomUid: UUID, userId: Long, userUid: UUID) {
        rooms[roomUid]?.let {
            it.members.remove(userId)
            publishSystem(roomUid, "leave", userUid)
            // 아무도 없으면 정리
            if (it.members.isEmpty()) {
                rooms.remove(roomUid)?.sink?.tryEmitComplete()
            }
        }
    }

    fun memberCount(roomUid: UUID): Int = channel(roomUid).members.size

    private fun channel(roomUid: UUID): RoomChannel =
        rooms.computeIfAbsent(roomUid) {
            RoomChannel(Sinks.many().multicast().onBackpressureBuffer())
        }

    private fun publishSystem(roomUid: UUID, event: String, userUid: UUID) {
        val json = """
            {"type":"system","event":"$event","userUid":$userUid,"at":"${OffsetDateTime.now()}"}
        """.trimIndent()
        publish(roomUid, json)
    }
}
