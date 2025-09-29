package kr.jiasoft.hiteen.feature.mbti.config

import kr.jiasoft.hiteen.feature.mbti.domain.MbtiQuestion
import kr.jiasoft.hiteen.feature.mbti.domain.MbtiResult
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "mbti")
data class MbtiConfig(
    var questions: List<MbtiQuestion> = emptyList(),
    var results: Map<String, MbtiResult> = emptyMap()
)
