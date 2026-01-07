package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.OffsetDateTime
import java.util.UUID

data class AdminFollowResponse(
    val followId: Long,

    val fromUserId: Long,
    val fromUserUid: UUID,
    val fromNickname: String?,
    val fromPhone: String?,

    val toUserId: Long,
    val toUserUid: UUID,
    val toNickname: String?,
    val toPhone: String?,

    val status: String,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val statusAt: OffsetDateTime?,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime?
)
