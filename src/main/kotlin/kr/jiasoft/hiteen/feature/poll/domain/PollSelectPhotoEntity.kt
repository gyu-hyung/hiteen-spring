package kr.jiasoft.hiteen.feature.poll.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.*

@Table("poll_select_photos")
data class PollSelectPhotoEntity(
    @Id
    val id: Long? = null,
    val selectId: Long,
    val assetUid: UUID?,
    val seq: Short = 0,
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
