package kr.jiasoft.hiteen.feature.school.domain

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "kakao")
data class KakaoProperties(
    var baseUrl: String = "",
    var apiKey: String = ""
)
