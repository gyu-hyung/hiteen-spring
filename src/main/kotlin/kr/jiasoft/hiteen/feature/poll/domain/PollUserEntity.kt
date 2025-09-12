package kr.jiasoft.hiteen.feature.poll.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("poll_users")
data class PollUserEntity (
    @Id
    val id: Long? = null,
    val pollId: Long,
    val userId: Long,
    val seq: Int?,
    val votedAt: OffsetDateTime,
)