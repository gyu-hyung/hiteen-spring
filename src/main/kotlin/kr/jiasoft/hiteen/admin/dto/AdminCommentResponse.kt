package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.OffsetDateTime
import java.util.UUID

data class AdminCommentResponse (
    val id: Long,
    val fkId: Long,
    val uid: UUID,
    val parentId: Long? = null,
    val replyCount: Int,
    val reportCount: Int,
    val likeCount: Int,
    val nickname: String,
    val content: String,
    val boardContent: String? = null,
    val status: String,
    val type: String,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime,
)