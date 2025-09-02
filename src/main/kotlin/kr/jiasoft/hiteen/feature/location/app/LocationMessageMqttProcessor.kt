package kr.jiasoft.hiteen.feature.location.app

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kr.jiasoft.hiteen.feature.integration.mqtt.dto.MqttResponse
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class LocationMessageMqttProcessor(
    private val objectMapper: ObjectMapper,
    private val locationService: LocationService
) : Processor {

    private val log = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    override fun process(exchange: Exchange) {
        val topic =
            exchange.`in`.getHeader("CamelMqtt5Topic", String::class.java)
                ?: exchange.`in`.getHeader("CamelMqttTopic", String::class.java)
                ?: exchange.`in`.getHeader("PahoMqtt5Topic", String::class.java)
                ?: "unknown"

        val payload = exchange.`in`.getBody(String::class.java)

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