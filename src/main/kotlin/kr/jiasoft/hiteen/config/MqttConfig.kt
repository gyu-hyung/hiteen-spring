package kr.jiasoft.hiteen.config

import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.channel.ExecutorChannel
import org.springframework.integration.core.MessageProducer
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory
import org.springframework.integration.mqtt.core.MqttPahoClientFactory
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter
import org.springframework.messaging.MessageChannel
import java.util.concurrent.Executors

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
    fun mqttInputChannel(): MessageChannel = ExecutorChannel(Executors.newFixedThreadPool(1))


//    @Bean
//    fun errorChannel(): MessageChannel = ExecutorChannel(Executors.newFixedThreadPool(1))


    // 3. MQTT Inbound Channel Adapter Bean (핵심)
    // 브로커로부터 메시지를 수신하여 mqttInputChannel로 전달
    @Bean
    fun inboundAdapter(): MessageProducer {
        return MqttPahoMessageDrivenChannelAdapter(
            clientId,
            mqttClientFactory(),
            topic
        ).apply {
            setCompletionTimeout(5000)
            setConverter(DefaultPahoMessageConverter())
            setQos(1)
            outputChannel = mqttInputChannel()
        }
    }

}