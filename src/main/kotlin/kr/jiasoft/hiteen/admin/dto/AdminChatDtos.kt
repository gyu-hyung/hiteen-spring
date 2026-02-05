package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import kr.jiasoft.hiteen.feature.chat.domain.ChatRoomInviteMode
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

// 채팅방 정보
data class AdminChatRoomResponse(
    val id: Long = 0,
    val uid: UUID? = null,
    val lastUserId: Long? = null,
    val lastMessageId: Long? = null,
    val createdId: Long? = null,
    val updatedId: Long? = null,
    val deletedId: Long? = null,

    var roomName: String? = null,
    val assetUid: UUID? = null,
    val inviteMode: ChatRoomInviteMode = ChatRoomInviteMode.ALL_MEMBERS,

    val userActiveCount: Int = 0,
    val userDeletedCount: Int = 0,
    val lastMessage: AdminChatMessageResponse?,

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val createdAt: OffsetDateTime,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val updatedAt: OffsetDateTime? = null,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val deletedAt: OffsetDateTime? = null,
)

// 채팅 메시지 정보
data class AdminChatMessageResponse(
    val id: Long,
    val uid: UUID,
    val roomId: Long? = null,
    val roomUid: UUID? = null,
    val roomName: String? = null,
    val userId: Long? = null,
    val userUid: UUID? = null,
    val userName: String? = null,
    val userPhone: String? = null,
    val userAsset: String? = null,
    val kind: Int,
    val content: String? = null,
    val emojiCode: String? = null,
    val emojiCount: Int? = null,
    val emojiList: List<EmojiInfo>? = null,

    val userCount: Int? = 0,
    val readCount: Int? = 0,
    val unreadCount: Int? = 0,
    val assets: List<UUID?> = emptyList(),

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime,
    @get:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yy.MM.dd HH:mm:ss")
    val createdDate: String? = createdAt.format(DateTimeFormatter.ofPattern("yy.MM.dd HH:mm:ss")),

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val deletedAt: OffsetDateTime? = null,
    @get:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yy.MM.dd HH:mm:ss")
    val deletedDate: String? = deletedAt?.format(DateTimeFormatter.ofPattern("yy.MM.dd HH:mm:ss")),
)

// 채팅방 참여자 정보
data class AdminChatUserResponse(
    val id: Long,
    val chatRoomId: Long,

    val userId: Long,
    val userUid: UUID? = null,
    val userName: String? = null,
    val userPhone: String? = null,
    val assetUid: UUID? = null,

    val isOwner: String? = "N",
    val lastReadMessageId: Long?,
    val lastReadAt: OffsetDateTime?,
    val status: Int?,
    val push: Boolean? = null,

    val isLeaved: String? = "N",
    val userDeleted: String? = "N",

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val pushAt: OffsetDateTime? = null,

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val joiningAt: OffsetDateTime? = null,

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val leavingAt: OffsetDateTime? = null,

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val deletedAt: OffsetDateTime? = null,
)

data class AdminChatRoomDetailResponse(
    val room: AdminChatRoomResponse?,
    val users: List<AdminChatUserResponse>,
)

data class EmojiInfo(
    val code: String? = null,
    val col: String? = null,
    val assetUid: UUID?,
    val count: Int = 0,
)