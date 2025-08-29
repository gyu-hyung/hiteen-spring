package kr.jiasoft.hiteen.feature.chat.dto

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
    val assetUids: List<UUID>? = null   // (선택) 자산 도메인 uid
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
    val senderUserUid: UUID,
    val content: String?,
    val createdAt: OffsetDateTime?,
    val assets: List<MessageAssetSummary> = emptyList(),
    val unreadCount: Int? = null,
)

data class ReadersCountRow(
    @Column("message_id") val messageId: Long,
    @Column("reader_count") val readerCount: Long
)


data class MessageAssetSummary(
    val uid: UUID,
    val assetUid: UUID?,     // (선택)
    val width: Int?,
    val height: Int?,
)


data class RoomsSnapshotResponse(
    val cursor: Long,
    val rooms: List<RoomSummaryResponse>
)