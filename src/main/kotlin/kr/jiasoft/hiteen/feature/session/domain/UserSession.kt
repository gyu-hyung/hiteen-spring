package kr.jiasoft.hiteen.feature.session.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("user_session")
data class UserSession(
    @Id val id: Long? = null,
    val userId: Long,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime? = null,
    val durationMinutes: Int? = null,
    val status: SessionStatus = SessionStatus.ACTIVE
)

enum class SessionStatus { ACTIVE, CLOSED, TIMEOUT }
