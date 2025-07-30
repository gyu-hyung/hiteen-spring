package kr.jiasoft.hiteen.mqtt

import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.channel.DirectChannel
import org.springframework.integration.core.MessageProducer

import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory
import org.springframework.integration.mqtt.core.MqttPahoClientFactory
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter
import org.springframework.messaging.MessageChannel

@Configuration
class MqttConfig(
    @Value("\${mqtt.broker.uri}") private val brokerUri: String,
    @Value("\${mqtt.client.id}") private val clientId: String,
    @Value("\${mqtt.username:#{null}}") private val username: String?,
    @Value("\${mqtt.password:#{null}}") private val password: String?,
    @Value("\${mqtt.topic}") private val topic: String
) {
    // 1. MQTT 클라이언트 연결 설정을 위한 팩토리 Bean
    @Bean
    fun mqttClientFactory(): MqttPahoClientFactory {
        val factory = DefaultMqttPahoClientFactory()
        val options = MqttConnectOptions()
        options.serverURIs = arrayOf(brokerUri)
        // username과 password가 application.properties에 설정되어 있을 경우에만 적용
        username?.let { options.userName = it }
        password?.let { options.password = it.toCharArray() }
        options.isCleanSession = true // 클린 세션 설정
        factory.connectionOptions = options
        return factory
    }

    // 2. MQTT 메시지가 흘러갈 채널 Bean
    @Bean
    fun mqttInputChannel(): MessageChannel {
        return DirectChannel()
    }

    // 3. MQTT Inbound Channel Adapter Bean (핵심)
    // 브로커로부터 메시지를 수신하여 mqttInputChannel로 전달
    @Bean
    fun inboundAdapter(): MessageProducer {
        return MqttPahoMessageDrivenChannelAdapter(
            clientId,
            mqttClientFactory(),
            topic
        ).apply {
            setCompletionTimeout(5000) // 연결 타임아웃 5초
            setConverter(DefaultPahoMessageConverter())
            setQos(1) // Quality of Service 레벨 설정
            outputChannel = mqttInputChannel()
        }
    }
}