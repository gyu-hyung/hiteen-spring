package kr.jiasoft.hiteen.feature.level.domain

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "exp")
data class ExpProperties @ConstructorBinding constructor(
    val actions: Map<String, ExpActionProperty>
)

data class ExpActionProperty(
    val description: String,
    val points: Int,
    val dailyLimit: Int? = null,
)
