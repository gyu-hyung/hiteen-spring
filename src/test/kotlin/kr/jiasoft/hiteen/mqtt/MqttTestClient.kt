package kr.jiasoft.hiteen.mqtt

import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.springframework.stereotype.Component

@Component
class MqttTestClient {

    private val broker = "tcp://localhost:1883" // emqx 도커 기준
    private val clientId = MqttClient.generateClientId()
    private val topic = "test/topic"

    private val client = MqttClient(broker, clientId, null)

    fun connectAndTest() {
        val connOpts = MqttConnectOptions().apply {
            isCleanSession = true
        }
        client.connect(connOpts)
        println("Connected to broker: $broker")

        // 구독(Subscribe)
        client.subscribe(topic) { t, msg ->
            println("Received message: ${String(msg.payload)} on topic: $t")
        }

        // 발행(Publish)
        val message = MqttMessage("Hello from Spring Boot MQTT!".toByteArray())
        client.publish(topic, message)
        println("Message published!")

        // 잠시 대기(수신 확인 위해)
        Thread.sleep(2000)

        client.disconnect()
        println("Disconnected")
    }
}