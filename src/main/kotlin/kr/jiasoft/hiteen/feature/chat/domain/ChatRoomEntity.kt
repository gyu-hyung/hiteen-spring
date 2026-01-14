package kr.jiasoft.hiteen.feature.chat.domain

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

enum class ChatRoomInviteMode {
    OWNER,
    ALL_MEMBERS,
}

@Schema(description = "채팅방 엔티티")
@Table("chat_rooms")
data class ChatRoomEntity(

    @param:Schema(description = "채팅방 PK", example = "1")
    @Id
    val id: Long = 0,

    @param:Schema(description = "채팅방 UID", example = "550e8400-e29b-41d4-a716-446655440000")
    val uid: UUID = UUID.randomUUID(),

    @param:Schema(description = "마지막 메시지를 작성한 사용자 ID", example = "1001")
    @Column("last_user_id")
    val lastUserId: Long? = null,

    @param:Schema(description = "마지막 메시지 ID", example = "5001")
    @Column("last_message_id")
    val lastMessageId: Long? = null,

    @param:Schema(description = "생성자 ID", example = "1001")
    @Column("created_id")
    val createdId: Long,

    @param:Schema(description = "채팅방 생성 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    @Column("created_at")
    val createdAt: OffsetDateTime,

    @param:Schema(description = "수정자 ID", example = "1002")
    @Column("updated_id")
    val updatedId: Long? = null,


    @param:Schema(description = "채팅방 수정 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    @Column("updated_at")
    val updatedAt: OffsetDateTime? = null,

    @param:Schema(description = "삭제자 ID", example = "1003")
    @Column("deleted_id")
    val deletedId: Long? = null,

    @param:Schema(description = "채팅방 삭제 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    @Column("deleted_at")
    val deletedAt: OffsetDateTime? = null,

    @param:Schema(description = "채팅방 이름", example = "우리반 단톡")
    @Column("room_name")
    val roomName: String? = null,

    @param:Schema(description = "채팅방 썸네일 asset uid", example = "f580e8e8-adee-4285-b181-3fed545e7be0")
    @Column("asset_uid")
    val assetUid: UUID? = null,

    @param:Schema(description = "채팅방 친구 초대 권한", example = "OWNER | ALL_MEMBERS")
    @Column("invite_mode")
    val inviteMode: ChatRoomInviteMode = ChatRoomInviteMode.ALL_MEMBERS,
)
