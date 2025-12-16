package kr.jiasoft.hiteen.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories

@Configuration
@EnableR2dbcRepositories(
    basePackages = [
        "kr.jiasoft.hiteen.feature.*",
        "kr.jiasoft.hiteen.admin.*"
    ]
)
@EnableReactiveMongoRepositories(
    basePackages = [
        "kr.jiasoft.hiteen.feature.location.infra.db"
    ]
)
@EnableRedisRepositories(
    basePackages = []
)
class PersistenceConfig
