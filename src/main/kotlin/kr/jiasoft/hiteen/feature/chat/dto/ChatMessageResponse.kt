package kr.jiasoft.hiteen.feature.chat.dto

import java.util.UUID
import java.time.OffsetDateTime

data class ChatMessageResponse(
    val messageUid: UUID,
    val roomUid: UUID,
    val userUid: UUID,
    val content: String?,
    val kind: Int,
    val emojiCode: String?,
    val createdAt: OffsetDateTime,
    val assetUids: List<UUID>? = emptyList()
)