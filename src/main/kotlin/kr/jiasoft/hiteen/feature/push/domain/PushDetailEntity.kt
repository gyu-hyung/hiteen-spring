package kr.jiasoft.hiteen.feature.push.domain

import com.fasterxml.jackson.annotation.JsonFormat
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("push_detail")
data class PushDetailEntity(
    @Id
    val id: Long = 0L,

    val pushId: Long?,               // FK to tb_push
    val userId: Long? = null,
    val deviceOs: String? = null,
    val deviceToken: String? = null,
    val phone: String? = null,

    val multicastId: String? = null,
    val messageId: String? = null,
    val registrationId: String? = null,

    val error: String? = null,
    val success: Int = 0,

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime? = null,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val updatedAt: OffsetDateTime? = null,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val deletedAt: OffsetDateTime? = null,
)
