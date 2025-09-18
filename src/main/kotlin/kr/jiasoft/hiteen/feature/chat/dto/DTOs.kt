package kr.jiasoft.hiteen.feature.chat.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.chat.domain.ChatMessageEntity
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
)


@Schema(description = "메시지 전송 요청 DTO")
data class SendMessageRequest(

    @param:Schema(description = "메시지 내용")
    val content: String? = null,

    @param:Schema(description = "첨부 파일 UIDs", example = "[a1a8990f-2443-4492-baad-699d59b272fa,a1a8990f-2443-4492-baad-699d59b272fa]")
    val assetUids: List<UUID>? = null,

    @param:Schema(description = "메시지 종류", example = "0")
    val kind: Short = 0,

    @param:Schema(description = "이모지 코드", example = "E_001")
    val emojiCode: String? = null,
)

@Schema(description = "채팅방 요약 응답 DTO")
data class RoomSummaryResponse(

    @param:Schema(description = "채팅방 UID", example = "550e8400-e29b-41d4-a716-446655440000")
    val roomUid: UUID,

    @param:Schema(description = "방에 참여한 멤버 수", example = "5")
    val memberCount: Int,

    @param:Schema(description = "읽지 않은 메시지 수", example = "2")
    val unreadCount: Int,

    @param:Schema(description = "마지막 업데이트 시각", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val updatedAt: OffsetDateTime?,

    @param:Schema(description = "마지막 메시지 요약 정보")
    val lastMessage: MessageSummary?
)

@Schema(description = "메시지 요약 DTO")
data class MessageSummary(

    @param:Schema(description = "메시지 UID", example = "b2a1f4a1-23b3-4c8f-9a7e-9e3d94c5f3a4")
    val messageUid: UUID,

    @param:Schema(description = "메시지 내용", example = "안녕하세요!")
    val content: String?,

    @param:Schema(description = "메시지 종류 (0=일반, 1=이미지, 2=이모지 등)", example = "0")
    val kind: Short = 0,

    @param:Schema(description = "이모지 코드", example = "👍")
    val emojiCode: String? = null,

    @param:Schema(description = "읽지 않은 사용자 수", example = "3")
    val unreadCount: Int? = null,

    @param:Schema(description = "메세지 작성 시각", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val createdAt: OffsetDateTime?,

    @param:Schema(description = "보낸 사람 정보")
    val sender: UserSummary?,

    @param:Schema(description = "첨부된 메시지 자산 목록")
    val assets: List<MessageAssetSummary>?,
) {
    companion object {
            fun from(entity: ChatMessageEntity, sender: UserSummary?, assets: List<MessageAssetSummary>?, unreadCount: Int? = null): MessageSummary {
            return MessageSummary(
                messageUid = entity.uid,
                content = entity.content,
                kind = entity.kind,
                emojiCode = entity.emojiCode,
                unreadCount = unreadCount,
                createdAt = entity.createdAt,
                sender = sender,
                assets = assets,
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
    val assetUid: UUID?,
//    val messageId: Long?,

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