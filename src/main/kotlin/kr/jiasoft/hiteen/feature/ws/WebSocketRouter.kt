package kr.jiasoft.hiteen.feature.ws

import kr.jiasoft.hiteen.feature.location.LocationWebSocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping


@Configuration
class WebSocketRouter(
    private val locationWebSocketHandler: LocationWebSocketHandler
) {
    @Bean
    fun webSocketMapping(): HandlerMapping {
        val map = mapOf("/ws/loc" to locationWebSocketHandler)
        return SimpleUrlHandlerMapping(map, -1)
    }
}