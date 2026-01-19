package kr.jiasoft.hiteen.feature.push.domain

import com.fasterxml.jackson.annotation.JsonFormat
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

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val updatedAt: OffsetDateTime? = null,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val deletedAt: OffsetDateTime? = null,
)
