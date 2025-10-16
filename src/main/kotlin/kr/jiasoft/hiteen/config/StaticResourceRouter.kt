package kr.jiasoft.hiteen.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router

@Configuration
class StaticResourceRouter {

    @Bean
    fun staticResourceRoutes(): RouterFunction<ServerResponse> = router {
        // /assets/** 요청을 classpath:/assets/로 매핑
        resources("/assets/**", ClassPathResource("assets/"))
    }
}
