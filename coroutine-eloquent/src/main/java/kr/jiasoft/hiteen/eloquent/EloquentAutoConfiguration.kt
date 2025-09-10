package kr.jiasoft.hiteen.eloquent

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate

@Configuration
@ConditionalOnClass(R2dbcEntityTemplate::class)
class EloquentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun relationRegistry() = RelationRegistryImpl()

    @Bean
    @ConditionalOnMissingBean
    fun coroutineEloquent(
        template: R2dbcEntityTemplate,
        relations: RelationRegistryImpl
    ) = CoroutineEloquent(template, relations)
}