package kr.jiasoft.hiteen.feature.poll.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("poll_likes")
data class PollLikeEntity (
    @Id
    val id: Long = 0,
    val pollId: Long,
    val userId: Long,
    val createdAt: OffsetDateTime,
)