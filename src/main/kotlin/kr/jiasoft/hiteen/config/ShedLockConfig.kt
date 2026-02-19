package kr.jiasoft.hiteen.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
class ShedLockConfig(
    @Value("\${spring.datasource.url:#{null}}") private val datasourceUrl: String?,
    @Value("\${spring.liquibase.url}") private val liquibaseUrl: String,
    @Value("\${spring.liquibase.user}") private val liquibaseUser: String,
    @Value("\${spring.liquibase.password}") private val liquibasePassword: String
) {

    /**
     * ShedLock 전용 DataSource (Liquibase 설정 재활용)
     */
    @Bean
    fun shedLockDataSource(): DataSource {
        val config = HikariConfig().apply {
            // datasource 설정이 있으면 사용, 없으면 liquibase 설정 사용
            jdbcUrl = datasourceUrl ?: liquibaseUrl
            username = liquibaseUser
            password = liquibasePassword
            maximumPoolSize = 3  // ShedLock은 커넥션 많이 필요 없음
            minimumIdle = 1
            connectionTimeout = 10000
            poolName = "ShedLock-Pool"
        }
        return HikariDataSource(config)
    }

    @Bean
    fun lockProvider(shedLockDataSource: DataSource): LockProvider {
        return JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(JdbcTemplate(shedLockDataSource))
                .usingDbTime()
                .build()
        )
    }
}

