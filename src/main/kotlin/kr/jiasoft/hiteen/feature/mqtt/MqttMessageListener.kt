package kr.jiasoft.hiteen.feature.mqtt

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kr.jiasoft.hiteen.feature.location.LocationService
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

@Service
class MqttMessageListener(
    private val objectMapper: ObjectMapper,
    private val locationService: LocationService,
    private val coroutineScope: CoroutineScope
) {

    @ServiceActivator(inputChannel = "mqttInputChannel")
    suspend fun handleMessage(
        @Payload payload: String, // 메시지 본문
        @Header("mqtt_receivedTopic") topic: String
    ) {
        println("============================================")
        println("topic = ${topic}")
        println("payload = ${payload}")
        println("============================================")
        coroutineScope.launch {
            try {
                locationService.saveLocationAsyncFromJson(json = payload)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

//    @ServiceActivator(inputChannel = "errorChannel")
//    fun errorHandler(message: Message<*>) {
//        // 예외 발생 시 알림, 로깅, 모니터링 등 처리
//        println("Error in MQTT message processing: ${message.payload}")
//    }
}