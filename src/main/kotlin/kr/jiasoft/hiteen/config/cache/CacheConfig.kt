package kr.jiasoft.hiteen.config.cache


import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfig {

    fun objectMapper(): ObjectMapper {
        val mapper = ObjectMapper()
        mapper.registerModule(JavaTimeModule())
        mapper.registerModule(KotlinModule.Builder().build())
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        mapper.activateDefaultTyping(
            mapper.polymorphicTypeValidator,
            ObjectMapper.DefaultTyping.EVERYTHING,
            JsonTypeInfo.As.WRAPPER_OBJECT
        );
        return mapper
    }

    // 일반 RedisConnectionFactory는 auto-config로 제공됨(Spring Boot)
    @Bean
    fun redisCacheManager(factory: RedisConnectionFactory): RedisCacheManager {

        val keySerializer = StringRedisSerializer()
        val valueSerializer = GenericJackson2JsonRedisSerializer(objectMapper())
        val key = RedisSerializationContext.SerializationPair.fromSerializer(keySerializer)
        val value = RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer)

        val config = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(key)
            .serializeValuesWith(value)
            .entryTtl(Duration.ofMinutes(10)) // 선택

        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build()
    }

}


