package kr.jiasoft.hiteen.config

import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
class ShedLockConfig {

    @Bean
    fun lockProvider(connectionFactory: RedisConnectionFactory): LockProvider {
        // Redis Cluster에서 모든 락 키가 같은 해시 슬롯에 저장되도록 {shedlock} 해시 태그 사용
        return RedisLockProvider(connectionFactory, "hiteen", "{shedlock}:")
    }
}

