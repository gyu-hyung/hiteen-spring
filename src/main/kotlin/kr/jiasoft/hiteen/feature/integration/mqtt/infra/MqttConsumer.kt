package kr.jiasoft.hiteen.feature.integration.mqtt.infra

import kr.jiasoft.hiteen.feature.location.app.LocationMessageMqttProcessor
import org.apache.camel.builder.RouteBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/** not used */
//@Component
class MqttConsumers(
    private val processor: LocationMessageMqttProcessor,
    @Value("\${mqtt.topic}") private val topic: String,
    @Value("\${mqtt.broker.uri}") private val brokerUri: String,
    @Value("\${mqtt.client.id}") private val clientId: String,
    @Value("\${mqtt.username}") private val username: String,
    @Value("\${mqtt.password}") private val password: String,
    @Value("\${mqtt.cleanStart}") private val cleanStart: String,
    @Value("\${mqtt.automaticReconnect}") private val automaticReconnect: String,
    @Value("\${mqtt.sessionExpiryInterval}") private val sessionExpiryInterval: String,
    @Value("\${mqtt.keepAliveInterval}") private val keepAliveInterval: String,
    @Value("\${mqtt.qos}") private val qos: String
) : RouteBuilder() {

    override fun configure() {

        // 에러 시 로깅
        onException(Exception::class.java)
            .handled(true)
            .logHandled(true)
            .log("MQTT route error: \${exception.message}")

        // 공통 처리 라우트 (바디를 String으로 맞추고 프로세서 호출)
        from("direct:processLocation")
            .convertBodyTo(String::class.java)
            .process(processor)

        // Consumer A
        from(
            "paho-mqtt5:$topic" +
                    "?brokerUrl=$brokerUri" +
                    "&clientId=$clientId" +
                    "&userName=$username" +
                    "&password=$password" +
                    "&automaticReconnect=$automaticReconnect" +
                    "&cleanStart=$cleanStart" +
                    "&sessionExpiryInterval=$sessionExpiryInterval" +
                    "&keepAliveInterval=$keepAliveInterval" +
                    "&qos=$qos"
        )
            .routeId("mqtt-a1")
            .to("direct:processLocation")

    }
}
