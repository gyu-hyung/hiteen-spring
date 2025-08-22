package kr.jiasoft.hiteen.feature.location.app

import kr.jiasoft.hiteen.feature.location.infra.cache.LocationRedisSubscriber
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class LocationWebSocketHandler(
    private val locationRedisSubscriber: LocationRedisSubscriber,
) : WebSocketHandler {

    override fun handle(session: WebSocketSession): Mono<Void> {
        val query = session.handshakeInfo.uri.query.orEmpty()

        // users=kim,park,lee 형태 파싱
        val userIds = query
            .split("&")
            .asSequence()
            .mapNotNull { part ->
                val idx = part.indexOf('=')
                if (idx <= 0) null else part.substring(0, idx) to part.substring(idx + 1)
            }
            .filter { (k, _) -> k == "users" }
            .flatMap { (_, v) -> v.split(",") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()

        if (userIds.isEmpty()) {
            return session.close(CloseStatus.BAD_DATA)
        }

        return forward(session, userIds)
    }

    private fun forward(session: WebSocketSession, userIds: List<String>): Mono<Void> {
        val merged: Flux<String> = Flux.merge(
            userIds.map { userId ->
                locationRedisSubscriber.subscribeUserChannel(userId)
                    .onBackpressureLatest()
            }
        )
        val outgoing = merged.map { payload -> session.textMessage(payload) }
        val closeSignal = session.receive().then()
        return session.send(outgoing).and(closeSignal)
    }
}