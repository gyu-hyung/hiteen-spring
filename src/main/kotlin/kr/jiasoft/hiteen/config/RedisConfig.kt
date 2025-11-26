package kr.jiasoft.hiteen.config

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.RedisClusterConfiguration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer

@Configuration
class RedisConfig {

    @Bean
    fun redisConnectionFactory(properties: RedisProperties): RedisConnectionFactory {
        return if (properties.cluster?.nodes?.isNotEmpty() == true) {
            // 🚀 Cluster mode
            println("🔗 Redis Mode: CLUSTER → nodes = ${properties.cluster.nodes}")

            val config = RedisClusterConfiguration(properties.cluster.nodes).apply {
                maxRedirects = properties.cluster.maxRedirects
                properties.password?.let { setPassword(it) }
            }

            LettuceConnectionFactory(config)

        } else {
            // 🧩 Standalone mode (local)
            println("🔗 Redis Mode: STANDALONE → ${properties.host}:${properties.port}")

            val standalone = org.springframework.data.redis.connection.RedisStandaloneConfiguration().apply {
                hostName = properties.host
                port = properties.port
                properties.password?.let { setPassword(it) }
            }

            LettuceConnectionFactory(standalone)
        }
    }

    @Bean
    fun reactiveRedisConnectionFactory(factory: RedisConnectionFactory): ReactiveRedisConnectionFactory =
        factory as ReactiveRedisConnectionFactory

    @Bean
    fun reactiveStringRedisTemplate(cf: ReactiveRedisConnectionFactory) =
        ReactiveStringRedisTemplate(cf)

    @Bean
    fun redisListenerContainer(cf: ReactiveRedisConnectionFactory) =
        ReactiveRedisMessageListenerContainer(cf)

    @Bean
    fun check(factory: RedisConnectionFactory) = CommandLineRunner {
        println("🚀 Redis Factory = ${factory::class.simpleName}")
    }
}





//V1
//@Configuration
//class RedisConfig {
//    @Bean
//    fun reactiveStringRedisTemplate(cf: ReactiveRedisConnectionFactory) =
//        ReactiveStringRedisTemplate(cf)
//
//    @Bean
//    fun redisListenerContainer(cf: ReactiveRedisConnectionFactory) =
//        ReactiveRedisMessageListenerContainer(cf)
//
//    @Bean
//    fun checkRedis(redisConnectionFactory: RedisConnectionFactory) = CommandLineRunner {
//        println("🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀 Redis Connection: " + redisConnectionFactory::class.simpleName)
//    }
//
//}


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
