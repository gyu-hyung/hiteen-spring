package kr.jiasoft.hiteen.feature.poll.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("poll_comment_likes")
data class PollCommentLikeEntity (
    @Id
    val id: Long? = null,
    val commentId: Long,
    val userId: Long,
    val createdAt: OffsetDateTime? = null,
//    val updatedAt: OffsetDateTime? = null,
//    val deletedAt: OffsetDateTime? = null,
)