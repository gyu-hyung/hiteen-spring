package kr.jiasoft.hiteen.admin.dto

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
    val statusAt: OffsetDateTime?,
    val createdAt: OffsetDateTime?
)
