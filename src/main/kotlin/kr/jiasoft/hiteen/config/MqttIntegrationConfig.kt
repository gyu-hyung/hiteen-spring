//package kr.jiasoft.hiteen.config
//
//import com.fasterxml.jackson.databind.ObjectMapper
//import kr.jiasoft.hiteen.feature.location.LocationService
//import kr.jiasoft.hiteen.feature.mqtt.MqttResponse
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import org.eclipse.paho.client.mqttv3.MqttConnectOptions
//import org.springframework.beans.factory.annotation.Value
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//import org.springframework.integration.annotation.ServiceActivator
//import org.springframework.integration.channel.ExecutorChannel
//import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory
//import org.springframework.integration.mqtt.core.MqttPahoClientFactory
//import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter
//import org.springframework.messaging.MessageChannel
//import org.springframework.messaging.MessageHandler
//import java.util.concurrent.Executors
//
//@Configuration
//class MqttIntegrationConfig(
//    @Value("\${mqtt.broker.uri}") private val brokerUri: String,
//    @Value("\${mqtt.client.id}") private val clientId: String,
//    @Value("\${mqtt.username}") private val username: String,
//    @Value("\${mqtt.password}") private val password: String,
//    @Value("\${mqtt.topic}") private val topic: String,
//    private val objectMapper: ObjectMapper,
//    private val locationService: LocationService
//) {
//
//    @Bean
//    fun mqttClientFactory(): MqttPahoClientFactory {
//        val factory = DefaultMqttPahoClientFactory()
//        val options = MqttConnectOptions()
//        options.serverURIs = arrayOf(brokerUri)
//        options.userName = username
//        options.password = password.toCharArray()
//        options.isAutomaticReconnect = true
//        options.isCleanSession = true
//        factory.connectionOptions = options
//        return factory
//    }
//
//    @Bean
//    fun mqttInputChannel(): MessageChannel = ExecutorChannel(Executors.newFixedThreadPool(1))
//
//    //    @Bean
////    fun errorChannel(): MessageChannel = ExecutorChannel(Executors.newFixedThreadPool(1))
//
//    @Bean
//    fun mqttInbound(): MqttPahoMessageDrivenChannelAdapter {
//        val adapter = MqttPahoMessageDrivenChannelAdapter(
//            clientId,
//            mqttClientFactory(),
//            topic
//        )
//        adapter.setQos(1)
//        adapter.outputChannel = mqttInputChannel()
//        return adapter
//    }
//
//    @Bean
//    @ServiceActivator(inputChannel = "mqttInputChannel")
//    fun mqttMessageHandler(): MessageHandler {
//        val coroutineScope = CoroutineScope(Dispatchers.IO)
//        return MessageHandler { message ->
//            val topic = message.headers["mqtt_receivedTopic"] as String
//            val payload = message.payload as String
//            println("=== [MQTT 수신] ===")
//            println("Topic: $topic")
//            println("Payload: $payload")
//            coroutineScope.launch {
//                try {
//                    val mqttResponse = objectMapper.readValue(payload, MqttResponse::class.java)
//                    locationService.saveLocationFromMqtt(mqttResponse)
//                } catch (e: Exception) {
//                    println("[ERROR] MQTT 메시지 처리 실패: $e")
//                }
//            }
//        }
//    }
//
//    //    @ServiceActivator(inputChannel = "errorChannel")
////    fun errorHandler(message: Message<*>) {
////        // 예외 발생 시 알림, 로깅, 모니터링 등 처리
////        println("Error in MQTT message processing: ${message.payload}")
////    }
//
//}
