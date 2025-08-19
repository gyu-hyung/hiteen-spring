package kr.jiasoft.hiteen.feature.location

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class LocationWebSocketHandler(
    private val locationRedisSubscriber: LocationRedisSubscriber,
    private val objectMapper: ObjectMapper
) : WebSocketHandler {

    override fun handle(session: WebSocketSession): Mono<Void> {
        val uri = session.handshakeInfo.uri
        val queryParams = uri.query
            ?.split("&")
            ?.map { it.split("=") }
            ?.associate { it[0] to it.getOrElse(1) { "" } }
            ?: emptyMap()

        // 클라이언트가 구독할 userId 목록을 전달 (예: /ws/loc?users=kim,park,lee)
        val userIds = queryParams["users"]?.split(",")?.map { it.trim() } ?: emptyList()

        val messages = Flux.merge(userIds.map { userId ->
            locationRedisSubscriber.subscribeUserChannel(userId)
                .map { json ->
                    session.textMessage(json)
                }
        })

        return session.send(messages)
    }
}