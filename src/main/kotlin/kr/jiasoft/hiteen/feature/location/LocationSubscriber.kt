//package kr.jiasoft.hiteen.feature.location
//
//import com.fasterxml.jackson.databind.ObjectMapper
//import jakarta.annotation.PostConstruct
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.reactive.asFlow
//import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
//import org.springframework.data.redis.listener.ChannelTopic
//import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
//import org.springframework.messaging.simp.SimpMessagingTemplate
//import org.springframework.stereotype.Component
//
//@Component
//class LocationSubscriber(
//    connectionFactory: ReactiveRedisConnectionFactory,
//    private val objectMapper: ObjectMapper,
//    private val messagingTemplate: SimpMessagingTemplate
//) {
//    private val container = ReactiveRedisMessageListenerContainer(connectionFactory)
//    private val scope = CoroutineScope(Dispatchers.Default)
//
//    @PostConstruct
//    fun subscribe() {
//        val topic = ChannelTopic("loc:user:*")
//
//        scope.launch {
//            container.receive(topic)
//                .asFlow()
//                .collect { message ->
//                    val payload: String = message.message
//                    val event = objectMapper.readValue(payload, LocationEvent::class.java)
//
//                    messagingTemplate.convertAndSend("/topic/loc/${event.userId}",event)
//                }
//        }
//    }
//}