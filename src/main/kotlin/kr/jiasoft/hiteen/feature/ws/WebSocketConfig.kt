package kr.jiasoft.hiteen.feature.ws

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.socket.server.WebSocketService
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter

import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService

@Configuration
class WebSocketConfig {
    @Bean
    fun handlerAdapter(): WebSocketHandlerAdapter = WebSocketHandlerAdapter(webSocketService())

    @Bean
    fun webSocketService(): WebSocketService =
        HandshakeWebSocketService(ReactorNettyRequestUpgradeStrategy())
}