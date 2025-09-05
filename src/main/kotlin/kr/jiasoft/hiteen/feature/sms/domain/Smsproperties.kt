package kr.jiasoft.hiteen.feature.sms.domain

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "sms")
class SmsProperties {
    lateinit var callback: String
    lateinit var apiKey: String
    lateinit var apiUrl: String
    lateinit var userId: String

    var kakao: Kakao = Kakao()

    class Kakao {
        var senderKey: String? = null
        var tokenUrl: String? = null
        var alimtalkUrl: String? = null
        var detailUrl: String? = null
    }
}
