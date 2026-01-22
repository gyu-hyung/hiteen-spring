package kr.jiasoft.hiteen.feature.chat.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import kr.jiasoft.hiteen.feature.chat.domain.ChatMessageEntity
import kr.jiasoft.hiteen.feature.chat.domain.ChatRoomInviteMode
import kr.jiasoft.hiteen.feature.user.dto.UserSummary
import org.springframework.data.relational.core.mapping.Column
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "단톡 방 생성 요청 DTO")
data class CreateRoomRequest(

    @param:Schema(description = "사용자 UUID", example = "[a1a8990f-2443-4492-baad-699d59b272fa,a1a8990f-2443-4492-baad-699d59b272fa]")
    val peerUids: List<UUID>, // 나를 제외한 멤버 UIDs. 최소 2명(= 나 포함하면 3명) 권장

    @param:Schema(description = "같은 멤버 채팅방 재사용 여부", example = "false")
    val reuseExactMembers: Boolean = false,

    @field:NotBlank(message = "채팅방 이름을 입력해줘~")
    @param:Schema(description = "채팅방 이름(room_name)", example = "우리반 단톡")
    val roomName: String? = null,

    @param:Schema(description = "채팅방 친구 초대 권한(invite_mode)", example = "ALL_MEMBERS")
    val inviteMode: ChatRoomInviteMode = ChatRoomInviteMode.OWNER,

//    @param:Schema(description = "채팅방 썸네일 asset uid(asset_uid)", example = "f580e8e8-adee-4285-b181-3fed545e7be0")
//    val assetUid: UUID? = null,
)


@Schema(description = "메시지 전송 요청 DTO")
data class SendMessageRequest(

    @param:Schema(description = "메시지 내용")
    val content: String? = null,

//    @param:Schema(description = "첨부 파일 UIDs", example = "[a1a8990f-2443-4492-baad-699d59b272fa,a1a8990f-2443-4492-baad-699d59b272fa]")
//    val assetUids: List<UUID>? = null,

//    @param:Schema(description = "메시지 종류", example = "0")
//    val kind: Int = 0,

    @param:Schema(description = "이모지 코드", example = "E_001")
    val emojiCode: String? = null,

    @param:Schema(description = "이모지 개수", example = "1")
    val emojiCount: Int? = null,

    @param:Schema(description = "이모지별 개수 집계 DTO")
    val emojiList: List<EmojisCountRow>? = null,
)
//{
//    @get:Schema(description = "메시지 종류 (자동 결정: 0=텍스트, 1=이모지, 2=이미지)")
//    val kind: Int
//        get() = if (emojiCode != null) 1 else 0
//}

@Schema(description = "이모지별 개수 집계 DTO")
data class EmojisCountRow(
    @Column("emoji_code") val emojiCode: String,
    @Column("emoji_count") val emojiCount: Int,
)

@Schema(description = "채팅방 요약 응답 DTO")
data class RoomSummaryResponse(

    @param:Schema(description = "채팅방 UID", example = "550e8400-e29b-41d4-a716-446655440000")
    val roomUid: UUID,

    @param:Schema(description = "채팅방 제목", example = "홍길동")
    val roomTitle: String,

    @param:Schema(description = "방에 참여한 멤버 수", example = "5")
    val memberCount: Int,

    @param:Schema(description = "읽지 않은 메시지 수", example = "2")
    val unreadCount: Int,

    @param:Schema(description = "프로필 이미지 UID", example = "f580e8e8-adee-4285-b181-3fed545e7be0")
    val assetUid: String?,

    @param:Schema(description = "마지막 업데이트 시각", example = "2025.09.18 10:15:30")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val updatedAt: OffsetDateTime?,

    @param:Schema(description = "마지막 메시지 요약 정보")
    val lastMessage: MessageSummary?
)

@Schema(description = "메시지 요약 DTO")
data class MessageSummary(

    @param:Schema(description = "메시지 UID", example = "b2a1f4a1-23b3-4c8f-9a7e-9e3d94c5f3a4")
    val messageUid: UUID,

    @param:Schema(description = "룸 UID", example = "b2a1f4a1-23b3-4c8f-9a7e-9e3d94c5f3a4")
    val roomUid: UUID,

    @param:Schema(description = "메시지 내용", example = "안녕!")
    val content: String?,

    @param:Schema(description = "메시지 종류 (0=텍스트, 1=이모지, 2=이미지, 4=시스템)", example = "0")
    val kind: Int,

    @param:Schema(description = "이모지 코드", example = "E_001")
    val emojiCode: String? = null,

    @param:Schema(description = "이모지 개수", example = "1")
    val emojiCount: Int? = null,

    @param:Schema(description = "이모지별 개수 집계 DTO")
    val emojiList: List<EmojisCountRow>? = null,

    @param:Schema(description = "발신 시각", example = "2025.09.18 10:15:30")
//    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime,

    @param:Schema(description = "발신자 정보")
    val sender: UserSummary?,

    @param:Schema(description = "첨부된 에셋(이미지 등) 목록")
    val assets: List<MessageAssetSummary> = emptyList(),

    @param:Schema(description = "읽지 않은 멤버 수", example = "3")
    val unreadCount: Int? = 0
) {
    companion object {
        fun from(entity: ChatMessageEntity, sender: UserSummary?, assets: List<MessageAssetSummary>?, unreadCount: Int? = null, roomUid: UUID, emojiList: List<EmojisCountRow>? = null): MessageSummary {
            return MessageSummary(
                messageUid = entity.uid,
                content = entity.content,
                kind = entity.kind,
                emojiCode = entity.emojiCode,
                emojiCount = entity.emojiCount,
                emojiList = emojiList,
                unreadCount = unreadCount,
                createdAt = entity.createdAt,
                sender = sender,
                assets = assets ?: emptyList(),
                roomUid = roomUid,
            )
        }
    }
}

data class ReadersCountRow(
    @Column("message_id") val messageId: Long,
    @Column("reader_count") val readerCount: Long
)

data class ActiveUsersRow(
    @Column("user_id") val userId: Long,
    @Column("user_uid") val userUid: UUID,
)


@Schema(description = "메세지 파일 요약 정보 DTO")
data class MessageAssetSummary(

    @param:Schema(description = "첨부파일 UUID", example = "a1a8990f-2443-4492-baad-699d59b272fa")
    val uid: UUID?,

    @param:Schema(description = "첨부파일 너비", example = "100")
    val width: Int?,

    @param:Schema(description = "첨부파일 높이", example = "100")
    val height: Int?,
)

@Schema(description = "채팅방 목록 정보 DTO")
data class RoomsSnapshotResponse(

    @param:Schema(description = "UUID 기반 커서", example = "a1a8990f-2443-4492-baad-699d59b272fa")
    val cursor: Long,

    @param:Schema(description = "채팅방 목록 정보")
    val rooms: List<RoomSummaryResponse>
)


@Schema(description = "채팅방 정보 DTO")
data class ChatRoomResponse(

    @param:Schema(description = "채팅방 PK", example = "1")
    val id: Long = 0,

    @param:Schema(description = "채팅방 UID", example = "550e8400-e29b-41d4-a716-446655440000")
    val uid: UUID = UUID.randomUUID(),

    @param:Schema(description = "마지막 메시지를 작성한 사용자 ID", example = "1001")
    val lastUserId: Long? = null,

    @param:Schema(description = "마지막 메시지 ID", example = "5001")
    val lastMessageId: Long? = null,

    @param:Schema(description = "생성자 ID", example = "1001")
    val createdId: Long,

    @param:Schema(description = "채팅방 생성 일시", example = "2025.09.18 10:15:30")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime,

    @param:Schema(description = "수정자 ID", example = "1002")
    val updatedId: Long? = null,

    @param:Schema(description = "채팅방 수정 일시", example = "2025.09.18 10:15:30")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val updatedAt: OffsetDateTime? = null,

    @param:Schema(description = "삭제자 ID", example = "1003")
    val deletedId: Long? = null,

    @param:Schema(description = "채팅방 삭제 일시", example = "2025.09.18 10:15:30")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val deletedAt: OffsetDateTime? = null,

    @param:Schema(description = "채팅방 이름", example = "우리반 단톡")
    val roomName: String,

    @param:Schema(description = "채팅방 썸네일 asset uid", example = "f580e8e8-adee-4285-b181-3fed545e7be0")
    val assetUid: UUID? = null,

    @param:Schema(description = "채팅방 친구 초대 권한", example = "OWNER | ALL_MEMBERS")
    val inviteMode: ChatRoomInviteMode = ChatRoomInviteMode.ALL_MEMBERS,
)


@Schema(description = "채팅방 참여 유저 정보(상세조회)")
data class ChatRoomMemberResponse(

//    @param:Schema(description = "user_id", example = "1001")
//    @Column("user_id")
//    val userId: Long,

    @param:Schema(description = "user_uid", example = "550e8400-e29b-41d4-a716-446655440000")
    @Column("user_uid")
    val userUid: UUID,

    @param:Schema(description = "chat_user_id", example = "123")
    @Column("chat_user_id")
    val chatUserId: Long,

    @param:Schema(description = "프로필 이미지 asset uid", example = "f580e8e8-adee-4285-b181-3fed545e7be0")
    @Column("asset_uid")
    val assetUid: UUID? = null,

    @param:Schema(description = "닉네임", example = "홍길동")
    @Column("nickname")
    val nickname: String? = null,

    @param:Schema(description = "해당 채팅방의 방장(생성자) 여부", example = "false")
    @Column("is_owner")
    val isOwner: Boolean,
)


@Schema(description = "채팅방 상세 조회 응답")
data class ChatRoomDetailResponse(
    @param:Schema(description = "채팅방 정보")
    val room: ChatRoomResponse,

    @param:Schema(description = "참여중인 유저 목록")
    val members: List<ChatRoomMemberResponse>,

    @param:Schema(description = "최근 발생한 시스템 메세지 (초대 등)")
    val systemMessage: MessageSummary? = null,
)

/**
 * listRoomsSnapshot 최적화용 프로젝션
 */
data class RoomSummaryProjection(
    val id: Long,
    val roomUid: UUID,
    val roomTitle: String?,
    val memberCount: Int?,
    val unreadCount: Int?,
    val assetUid: String?,
    val updatedAt: OffsetDateTime?,

    // Last Message Fields
    val lastMessageId: Long?,
    val lastMessageUid: UUID?,
    val lastContent: String?,
    val lastKind: Int?,
    val lastEmojiCode: String?,
    val lastEmojiCount: Int?,
    val lastCreatedAt: OffsetDateTime?,

    // Last Message Sender Fields
    val lastSenderId: Long?,
    val lastSenderUid: UUID?,
    val lastSenderUsername: String?,
    val lastSenderNickname: String?,
    val lastSenderAssetUid: String?
)

/**
 * pageMessages 최적화용 프로젝션
 */
data class MessageSummaryProjection(
    val id: Long,
    val messageUid: UUID,
    val userId: Long,
    val content: String?,
    val kind: Int,
    val emojiCode: String?,
    val emojiCount: Int?,
    val createdAt: OffsetDateTime,

    // Sender info
    val senderId: Long,
    val senderUid: UUID,
    val senderUsername: String,
    val senderNickname: String?,
    val senderAssetUid: String?,
)


@Schema(description = "채팅방 초대 요청")
data class ChatRoomInviteRequest(
    @param:Schema(description = "초대할 사용자 UID 목록", example = "[\"550e8400-e29b-41d4-a716-446655440000\"]")
    val peerUids: List<UUID>
)

/**
 * ChatUserRepository.findAllDetailedByRoomIds 용 프로젝션
 */
data class ChatUserNicknameProjection(
    val id: Long,
    val chatRoomId: Long,
    val userId: Long,
    val lastReadMessageId: Long?,
    val lastReadAt: OffsetDateTime?,
    val status: Int?,
    val push: Boolean?,
    val pushAt: OffsetDateTime?,
    val joiningAt: OffsetDateTime?,
    val leavingAt: OffsetDateTime?,
    val deletedAt: OffsetDateTime?,
    val nickname: String
)
