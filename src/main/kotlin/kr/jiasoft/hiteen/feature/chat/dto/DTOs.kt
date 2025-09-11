package kr.jiasoft.hiteen.feature.chat.dto

import kr.jiasoft.hiteen.feature.user.dto.UserResponse
import org.springframework.data.relational.core.mapping.Column
import java.time.OffsetDateTime
import java.util.UUID

data class CreateDirectRoomRequest(val peerUid: UUID)
data class CreateRoomRequest(
    val peerUids: List<UUID>, // 나를 제외한 멤버 UIDs. 최소 2명(= 나 포함하면 3명) 권장
    val reuseExactMembers: Boolean = false,
)
data class TogglePushRequest(val enabled: Boolean)

data class SendMessageRequest(
    val content: String? = null,
    val assetUids: List<UUID>? = null,
    val kind: Short = 0,
    val emojiCode: String? = null,
)

data class RoomSummaryResponse(
    val roomUid: UUID,
    val lastMessage: MessageSummary?,
    val memberCount: Int,
    val unreadCount: Int,
    val updatedAt: OffsetDateTime?
)

data class MessageSummary(
    val messageUid: UUID,
    val content: String?,
    val unreadCount: Int? = null,
    val createdAt: OffsetDateTime?,
    val sender: UserResponse?,
    val assets: List<MessageAssetSummary> = emptyList(),
)

data class ReadersCountRow(
    @Column("message_id") val messageId: Long,
    @Column("reader_count") val readerCount: Long
)

data class ActiveUsersRow(
    @Column("user_id") val userId: Long,
    @Column("user_uid") val userUid: UUID,
)


data class MessageAssetSummary(
//    val id: Long?,
    val assetUid: UUID?,
//    val messageId: Long?,
    val width: Int?,
    val height: Int?,
)


data class RoomsSnapshotResponse(
    val cursor: Long,
    val rooms: List<RoomSummaryResponse>
)