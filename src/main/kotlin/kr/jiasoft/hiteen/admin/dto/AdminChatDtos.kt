package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import kr.jiasoft.hiteen.feature.chat.domain.ChatRoomInviteMode
import org.springframework.data.relational.core.mapping.Column
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class AdminChatRoomResponse(
    val id: Long = 0,
    val uid: UUID? = null,
    val lastUserId: Long? = null,
    val lastMessageId: Long? = null,
    val createdId: Long,
    val updatedId: Long? = null,
    val deletedId: Long? = null,

    val roomName: String? = null,
    val assetUid: UUID? = null,
    val inviteMode: ChatRoomInviteMode = ChatRoomInviteMode.ALL_MEMBERS,

    val memberCount: Int = 0,
    val unreadCount: Int = 0,
    val lastMessage: AdminChatMessageResponse?,

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val updatedAt: OffsetDateTime? = null,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val deletedAt: OffsetDateTime? = null,
)

data class AdminChatMessageResponse(
    val id: Long,
    val uid: UUID,
    val roomId: Long,
    val roomUid: UUID,
    val roomName: String? = null,
    val userId: Long,
    val userName: String?,
    val userPhone: String?,
    val kind: Int,
    val content: String?,
    val emojiCode: String? = null,
    val emojiCount: Int? = null,
    val emojiList: List<EmojiInfo>? = null,

    val userCount: Int? = 0,
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

data class AdminChatUserResponse(
    val id: Long,
    val chatRoomId: Long,
    val userId: Long,
    val userUid: UUID,
    val assetUid: UUID? = null,
    val nickname: String? = null,
    val isOwner: Boolean,
    val lastReadMessageId: Long?,
    val lastReadAt: OffsetDateTime?,
    val status: Int?,
    val push: Boolean? = null,

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
    val room: AdminChatRoomResponse,
    val members: List<AdminChatUserResponse>,
    val systemMessage: AdminChatMessageResponse? = null,
)

data class AdminMessageAssetResponse(
    val uid: UUID?,
    val width: Int?,
    val height: Int?,
)

data class ReadersCountRow(
    @Column("message_id") val messageId: Long,
    @Column("reader_count") val readerCount: Long
)

data class ActiveUsersRow(
    @Column("user_id") val userId: Long,
    @Column("user_uid") val userUid: UUID,
)

data class EmojiInfo(
    val code: String? = null,
    val col: String? = null,
    val assetUid: UUID?,
    val count: Int = 0,
)