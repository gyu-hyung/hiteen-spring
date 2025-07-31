package kr.jiasoft.hiteen.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CoroutineConfig {
    @Bean
    fun applicationCoroutineScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
