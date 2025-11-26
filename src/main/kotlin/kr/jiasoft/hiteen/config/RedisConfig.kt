package kr.jiasoft.hiteen.config

import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.RedisConnectionFactory
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

    @Bean
    fun checkRedis(redisConnectionFactory: RedisConnectionFactory) = CommandLineRunner {
        println("🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀 Redis Connection: " + redisConnectionFactory::class.simpleName)
    }

}


//package kr.jiasoft.hiteen.config
//
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
//import org.springframework.data.redis.core.ReactiveStringRedisTemplate
//import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
//
//@Configuration
//class RedisConfig {
//
//    @Bean
//    fun reactiveStringRedisTemplate(factory: ReactiveRedisConnectionFactory) =
//        ReactiveStringRedisTemplate(factory)
//
//    @Bean
//    fun reactiveRedisMessageListenerContainer(factory: ReactiveRedisConnectionFactory) =
//        ReactiveRedisMessageListenerContainer(factory)
//}
