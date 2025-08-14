package kr.jiasoft.hiteen.feature.mqtt

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kr.jiasoft.hiteen.feature.location.LocationService
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class LocationMessageProcessor(
    private val objectMapper: ObjectMapper,
    private val locationService: LocationService
) : Processor {

    private val log = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    override fun process(exchange: Exchange) {
        // Camel paho-mqtt5의 토픽 헤더는 구현/버전에 따라 이름이 다를 수 있어 안전하게 조회
        val topic =
            exchange.`in`.getHeader("CamelMqtt5Topic", String::class.java)
                ?: exchange.`in`.getHeader("CamelMqttTopic", String::class.java)
                ?: exchange.`in`.getHeader("PahoMqtt5Topic", String::class.java)
                ?: "unknown"

        val payload = exchange.`in`.getBody(String::class.java) // 아래 라우트에서 convertBodyTo 보장

        log.debug("MQTT <- topic={} payload={}", topic, payload)

        scope.launch {
            try {
                val dto = objectMapper.readValue(payload, MqttResponse::class.java)
                locationService.saveLocationFromMqtt(dto)
            } catch (e: Exception) {
                log.warn("MQTT message handling failed: {}", e.message, e)
            }
        }
    }

    @PreDestroy
    fun shutdown() {
        scope.cancel()
    }
}
