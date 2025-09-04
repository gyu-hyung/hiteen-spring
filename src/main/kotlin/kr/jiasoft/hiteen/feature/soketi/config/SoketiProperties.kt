package kr.jiasoft.hiteen.feature.soketi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "soketi")
class SoketiProperties {
    var appId: String = "hiteen-id"
    var appKey: String = "hiteen-key"
    var appSecret: String = "hiteen-secret"
    var host: String = "127.0.0.1"
    var port: Int = 6001
    var encrypted: Boolean = false
    var jwtSecret: String? = null
}
