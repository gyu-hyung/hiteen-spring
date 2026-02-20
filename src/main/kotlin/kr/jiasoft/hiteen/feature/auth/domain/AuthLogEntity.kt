package kr.jiasoft.hiteen.feature.auth.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table(name = "auth_logs")
data class AuthLogEntity(
    @Id
    val id: Long = 0,
    val username: String?,
    val eventType: String, // LOGIN, LOGOUT, AUTH_FAIL, SESSION_INVALID, etc.
    val tokenId: String?,
    val clientIp: String?,
    val userAgent: String?,
    val details: String?,
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
