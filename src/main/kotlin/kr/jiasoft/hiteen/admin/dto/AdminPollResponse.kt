package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.OffsetDateTime
import java.util.UUID

data class AdminPollResponse (
    val id: Long,
    val question: String,
    val photo: UUID?,
    val voteCount: Int,
    val commentCount: Int,
    val likeCount: Int,
    val reportCount: Int,
    val allowComment: Int,
    val options: List<String>?,
    val startDate: String?,
    val endDate: String?,
    val status: String,
    val nickname: String,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime,
    val attachments: List<UUID>?,
)