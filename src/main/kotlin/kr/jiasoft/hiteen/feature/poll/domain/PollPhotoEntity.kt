package kr.jiasoft.hiteen.feature.poll.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("poll_photos")
data class PollPhotoEntity(
    @Id
    val id: Long? = null,
    val pollId: Long,
    val assetUid: UUID,
    val seq: Short = 0,
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
