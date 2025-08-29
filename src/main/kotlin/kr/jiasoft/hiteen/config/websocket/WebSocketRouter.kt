package kr.jiasoft.hiteen.config.websocket

import kr.jiasoft.hiteen.feature.chat.app.ChatWebSocketHandler
import kr.jiasoft.hiteen.feature.chat.app.InboxWebSocketHandler
import kr.jiasoft.hiteen.feature.location.app.LocationWebSocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping


@Configuration
class WebSocketRouter(
    private val locationWebSocketHandler: LocationWebSocketHandler,
    private val chatWebSocketHandler: ChatWebSocketHandler,
    private val inboxWebSocketHandler: InboxWebSocketHandler,
) {
    @Bean
    fun webSocketMapping(): HandlerMapping {
        return SimpleUrlHandlerMapping(
            mapOf(
                "/ws/loc" to locationWebSocketHandler,
                "/ws/chat" to chatWebSocketHandler,
                "/ws/inbox" to inboxWebSocketHandler,

                ),
            -1
        )
    }
}