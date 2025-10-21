package kr.jiasoft.hiteen.feature.poll.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("poll_selects")
data class PollSelectEntity(
    @Id
    val id: Long = 0,
    val pollId: Long,
    val seq: Int,
    val content: String,
    val voteCount: Int = 0,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime? = null
)
