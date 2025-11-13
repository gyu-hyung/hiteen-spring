package kr.jiasoft.hiteen.config.cache


import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.cache.support.CompositeCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfig (
    private val reactiveRedisConnectionFactory: ReactiveRedisConnectionFactory
) {

    @Bean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    /**
     * Redis CacheManager (동기 방식)
     * - Spring Cacheable에 사용 가능
     * - TTL 30분
     */
    @Bean
    fun redisCacheManager(objectMapper: ObjectMapper): RedisCacheManager {
        val serializer = GenericJackson2JsonRedisSerializer(objectMapper)

        val cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .disableCachingNullValues()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))

        val connectionFactory = reactiveRedisConnectionFactory as RedisConnectionFactory
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(cacheConfig)
            .build()
    }

//    @Bean
//    fun compositeCacheManager(
//        caffeineCacheManager: CaffeineCacheManager,
//        redisCacheManager: RedisCacheManager,
//    ): CompositeCacheManager {
//        val manager = CompositeCacheManager()
//        manager.setCacheManagers(listOf(caffeineCacheManager, redisCacheManager))
//        manager.setFallbackToNoOpCache(false)
//        return manager
//    }

    /**
     * Caffeine 기반 Spring Cache 설정
     * - asyncCacheMode = true → CompletableFuture / Mono / Flux 캐시 지원
     * - expireAfterWrite(10분)
     * - 최대 항목 1000개
     */
//    @Bean
//    fun caffeineCacheManager(): CaffeineCacheManager {
//        val manager = CaffeineCacheManager().apply {
//            isAllowNullValues = false         // null 캐시 비허용
//            setAsyncCacheMode(true)
//            setCaffeine(
//                Caffeine.newBuilder()
//                    .expireAfterWrite(14400, TimeUnit.MINUTES)
//                    .maximumSize(1000)
//            )
//        }
//
//        // 미리 등록할 캐시 이름들 (선택 사항)
////        manager.setCacheNames(listOf("userDetails", "authToken", "gameData"))
//        return manager
//    }
}


