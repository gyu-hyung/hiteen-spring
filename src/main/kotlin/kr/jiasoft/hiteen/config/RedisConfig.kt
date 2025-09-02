package kr.jiasoft.hiteen.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer

@Configuration
class RedisConfig {
    @Bean
    fun reactiveStringRedisTemplate(cf: ReactiveRedisConnectionFactory) =
        ReactiveStringRedisTemplate(cf)

    @Bean
    fun redisListenerContainer(cf: ReactiveRedisConnectionFactory) =
        ReactiveRedisMessageListenerContainer(cf)
}
