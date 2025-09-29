package kr.jiasoft.hiteen.feature.push.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("push")
data class PushEntity(
    @Id
    val id: Long = 0L,

    val type: String? = null,        // toast, dialog, notification
    val code: String? = null,
    val title: String? = null,
    val message: String? = null,

    val total: Long = 0,
    val success: Long = 0,
    val failure: Long = 0,

    val multicastId: String? = null,
    val canonicalIds: String? = null,

    val createdId: Long? = null,

    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime? = null,
    val deletedAt: OffsetDateTime? = null,
)
