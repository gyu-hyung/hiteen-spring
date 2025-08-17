package kr.jiasoft.hiteen.feature.tbmq.credential

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("mqtt_credentials")
data class MqttCredEntity(
    @Id val id: Long? = null,
    val userId: Long,
    val credentialsId: String,
    val clientId: String,
    val username: String,
    val password: String,
    val issuedAt: Instant,
    val expiresAt: Instant?
)
