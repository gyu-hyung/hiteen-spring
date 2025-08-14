package kr.jiasoft.hiteen.feature.tbmq

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig {
    @Bean
    fun tbmqWebClient(props: TbmqProperties): WebClient =
        WebClient.builder()
            .baseUrl(props.url)
            .exchangeStrategies(
                ExchangeStrategies.builder()
                    .codecs { it.defaultCodecs().maxInMemorySize(2 * 1024 * 1024) }
                    .build()
            )
            .build()
}
