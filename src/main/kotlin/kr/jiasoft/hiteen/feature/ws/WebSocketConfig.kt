//package kr.jiasoft.hiteen.feature.ws
//
//import org.springframework.context.annotation.Configuration
//import org.springframework.messaging.simp.config.MessageBrokerRegistry
//import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
//import org.springframework.web.socket.config.annotation.StompEndpointRegistry
//import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
//
//@Configuration
//@EnableWebSocketMessageBroker
//class WebSocketConfig : WebSocketMessageBrokerConfigurer {
//
//    // 클라이언트가 연결할 WebSocket 엔드포인트 등록
//    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
//        registry.addEndpoint("/ws") // ws://localhost:8080/ws
//            .setAllowedOriginPatterns("*") // 개발 중엔 전체 허용
//            .withSockJS() // SockJS fallback 지원 (옵션)
//    }
//
//    // 메시지 브로커 설정
//    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
//        // 서버에서 클라이언트로 보낼 prefix
//        registry.enableSimpleBroker("/topic")
//        // 클라이언트 → 서버 메시지 prefix
//        registry.setApplicationDestinationPrefixes("/app")
//    }
//}