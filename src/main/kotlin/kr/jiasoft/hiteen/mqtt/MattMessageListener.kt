package kr.jiasoft.hiteen.mqtt

import org.slf4j.LoggerFactory
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

@Service
class MqttMessageListener {

    private val logger = LoggerFactory.getLogger(MqttMessageListener::class.java)

    @ServiceActivator(inputChannel = "mqttInputChannel")
    fun handleMessage(
        @Payload payload: String, // 메시지 본문
        @Header("mqtt_receivedTopic") topic: String
    ) {
        logger.info("========================================")
        logger.info("MQTT Message Received!")
        logger.info("Topic: $topic")
        logger.info("Payload: $payload")
        logger.info("========================================")

        // 여기에 수신된 데이터를 처리하는 비즈니스 로직을 구현합니다.
        // 예: JSON 파싱, 데이터베이스 저장, 다른 서비스로 전달 등
    }
}