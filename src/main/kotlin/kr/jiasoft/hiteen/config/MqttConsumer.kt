package kr.jiasoft.hiteen.config

import kr.jiasoft.hiteen.feature.mqtt.LocationMessageProcessor
import org.apache.camel.builder.RouteBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class MqttConsumers(
    private val processor: LocationMessageProcessor,
    @Value("\${mqtt.topic}") private val topic: String,
    @Value("\${mqtt.broker.uri}") private val brokerUri: String,
    @Value("\${mqtt.client.id}") private val clientId: String,
    @Value("\${mqtt.username}") private val username: String,
    @Value("\${mqtt.password}") private val password: String
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
                    "&clientId=${clientId}" +
                    "&userName=$username" +
                    "&password=$password" +
                    "&automaticReconnect=true" +
                    "&cleanStart=false" +
                    "&sessionExpiryInterval=86400" +
                    "&keepAliveInterval=60" +
                    "&qos=1"
        )
            .routeId("mqtt-a1")
            .to("direct:processLocation")

        // Consumer B 여러개 가능
//        from(
//            "paho-mqtt5:\$share/group1/location/#" +
//                    "?brokerUrl={{mqtt.broker.uri}}" +
//                    "&clientId=a2" +
//                    "&userName=a2" +
//                    "&password=a2" +
//                    "&automaticReconnect=true" +
//                    "&cleanStart=false" +
//                    "&sessionExpiryInterval=86400" +
//                    "&keepAliveInterval=30" +
//                    "&qos=1"
//        )
//            .routeId("mqtt-a2")
//            .to("direct:processLocation")
    }
}
