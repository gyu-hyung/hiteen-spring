package kr.jiasoft.hiteen.config.cache
//import com.fasterxml.jackson.annotation.JsonTypeInfo
//import com.fasterxml.jackson.databind.ObjectMapper
//import com.fasterxml.jackson.databind.SerializationFeature
//import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
//import com.fasterxml.jackson.datatype.jsr310.deser.JavaTimeDeserializerModifier
//import com.fasterxml.jackson.module.kotlin.kotlinModule
//import org.springframework.cache.annotation.EnableCaching
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//import org.springframework.data.redis.cache.RedisCacheConfiguration
//import org.springframework.data.redis.cache.RedisCacheManager
//import org.springframework.data.redis.connection.RedisConnectionFactory
//import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
//import org.springframework.data.redis.serializer.RedisSerializationContext
//import org.springframework.data.redis.serializer.StringRedisSerializer
//import java.time.Duration
//
//@Configuration
//@EnableCaching
//class CacheConfig {
//
//
//
//    @Bean
//    fun cacheManager(redisConnectionFactory: RedisConnectionFactory): RedisCacheManager {
//        // 1. Jackson ObjectMapper 설정 (Polymorphic Type 지원)
//        val objectMapper = ObjectMapper().apply {
//            registerModule(kotlinModule())
//            registerModule(JavaTimeModule())
//                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
//        }
//
//        // 2. 기본 캐시 설정: JSON 직렬화, TTL 5분
//        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
//            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()))
//            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(GenericJackson2JsonRedisSerializer()))
//            .entryTtl(Duration.ofMinutes(5))
//
//        // 3. 캐시 이름("products")별로 TTL을 다르게 설정
//        val customConfigs = mapOf(
//            "userResponse" to defaultConfig.entryTtl(Duration.ofMinutes(10)),
//            "userEntity" to defaultConfig.entryTtl(Duration.ofMinutes(1))
//        )
//
//        return RedisCacheManager.builder(redisConnectionFactory)
//            .cacheDefaults(defaultConfig)
//            .withInitialCacheConfigurations(customConfigs)
//            .build()
//    }
//}



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


