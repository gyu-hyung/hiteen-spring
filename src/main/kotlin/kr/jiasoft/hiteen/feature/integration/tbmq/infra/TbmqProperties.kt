package kr.jiasoft.hiteen.feature.integration.tbmq.infra

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "tbmq")
class TbmqProperties {
    lateinit var url: String
    lateinit var admin: Admin
    lateinit var mqtt: Mqtt
    class Admin { lateinit var username: String; lateinit var password: String }
    class Mqtt { lateinit var host: String; var port: Int = 8883; var tls: Boolean = true }
}