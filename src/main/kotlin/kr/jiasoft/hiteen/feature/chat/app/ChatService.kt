package kr.jiasoft.hiteen.feature.chat.app

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.common.exception.BusinessValidationException
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.asset.domain.AssetCategory
import kr.jiasoft.hiteen.feature.asset.dto.AssetResponse
import kr.jiasoft.hiteen.feature.chat.domain.*
import kr.jiasoft.hiteen.feature.chat.dto.MessageAssetSummary
import kr.jiasoft.hiteen.feature.chat.dto.MessageSummary
import kr.jiasoft.hiteen.feature.chat.dto.RoomSummaryResponse
import kr.jiasoft.hiteen.feature.chat.dto.RoomsSnapshotResponse
import kr.jiasoft.hiteen.feature.chat.dto.SendMessageRequest
import kr.jiasoft.hiteen.feature.chat.infra.ChatMessageAssetRepository
import kr.jiasoft.hiteen.feature.chat.infra.ChatMessageRepository
import kr.jiasoft.hiteen.feature.chat.infra.ChatRoomRepository
import kr.jiasoft.hiteen.feature.chat.infra.ChatUserRepository
import kr.jiasoft.hiteen.feature.level.app.ExpService
import kr.jiasoft.hiteen.feature.push.app.PushService
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
import kr.jiasoft.hiteen.feature.push.domain.buildPushData
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
class ChatService(
    private val rooms: ChatRoomRepository,
    private val chatUsers: ChatUserRepository,
    private val messages: ChatMessageRepository,
    private val msgAssets: ChatMessageAssetRepository,
    private val users: UserRepository,
//    private val soketiBroadcaster: SoketiBroadcaster,

    private val expService: ExpService,
    private val pushService: PushService,
    private val assetService: AssetService,
) {

    /**
     * 해당 채팅방 회원인지
     */
//    private suspend fun assertMember(roomUid: UUID, userId: Long) : {
//        val room = rooms.findByUid(roomUid) ?: error("room not found")
//        val me = chatUsers.findActive(room.id, userId) ?: return
//    }


    /** DM 방 생성 TODO 친구가 맞는지? */
    suspend fun createDirectRoom(currentUserId: Long, peerUid: UUID): UUID {
        val peer = users.findByUid(peerUid.toString()) ?: error("peer not found")
        val existing = rooms.findDirectRoom(currentUserId, peer.id)
        if (existing != null) return existing.uid

        val saved = rooms.save(
            ChatRoomEntity(
                createdId = currentUserId,
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now()
            )
        )
        chatUsers.save(ChatUserEntity(chatRoomId = saved.id, userId = currentUserId, push = true, pushAt = OffsetDateTime.now(), joiningAt = OffsetDateTime.now()))
        chatUsers.save(ChatUserEntity(chatRoomId = saved.id, userId = peer.id, push = true, pushAt = OffsetDateTime.now(), joiningAt = OffsetDateTime.now()))
        return saved.uid
    }

    /** 단톡 방 생성 */
    suspend fun createRoom(currentUserId: Long, peerUids: List<UUID>, reuseExactMembers: Boolean): UUID {
        val peerIds = peerUids.distinct().map { uid ->
            users.findByUid(uid.toString())?.id ?: error("peer not found: $uid")
        }
        val memberIds = (peerIds + currentUserId).distinct()

        if (memberIds.size == 2) {
            val otherId = memberIds.first { it != currentUserId }
            val otherUid = users.findUidById(otherId) ?: error("peer not found: $otherId")
            return createDirectRoom(currentUserId, otherUid)
        }

        if (reuseExactMembers) {
            rooms.findRoomByExactActiveMembers(memberIds, memberIds.size)?.let { return it.uid }
        }

        val saved = rooms.save(
            ChatRoomEntity(
                createdId = currentUserId,
                createdAt = OffsetDateTime.now(),
            )
        )

        // 멤버 추가
        memberIds.forEach { uid ->
            chatUsers.save(
                ChatUserEntity(
                    chatRoomId = saved.id,
                    userId = uid,
                    push = true,
                    pushAt = OffsetDateTime.now(),
                    joiningAt = OffsetDateTime.now()
                )
            )
        }

        return saved.uid
    }


    /** 방 조회 TODO 상세 구현(메세지 목록?, 참여 멤버?)*/
    suspend fun getRoomByUid(roomUid: UUID): ChatRoomEntity =
        rooms.findByUid(roomUid) ?: error("room not found")


    /** 메시지 전송 */
    suspend fun sendMessage(
        roomUid: UUID,
        sendUser: UserEntity,
        req: SendMessageRequest,
        files: List<FilePart>
    ): MessageSummary {

        val room = rooms.findByUidAndDeletedAtIsNull(roomUid)
            ?: throw IllegalArgumentException("room not found")
        chatUsers.findActive(room.id, sendUser.id)
            ?: throw IllegalArgumentException("not a member")

        // 메시지 저장
        val savedMsg = messages.save(
            ChatMessageEntity(
                chatRoomId = room.id,
                userId = sendUser.id,
                content = req.content,
                kind = when {
                    files.isNotEmpty() -> 2
                    req.emojiCode != null -> 1
                    else -> 0
                },
                emojiCode = req.emojiCode,
                createdAt = OffsetDateTime.now(),
            )
        )

//        val uploaded: List<AssetResponse> =
        if (files.isNotEmpty())
            assetService.uploadImages(files, sendUser.id, AssetCategory.CHAT_MESSAGE).toList()
                .forEach { asset ->
                    msgAssets.save(
                        ChatMessageAssetEntity(
                            uid = asset.uid,
                            messageId = savedMsg.id,
                            width = asset.width,
                            height = asset.height,
                        )
                    )
        }

        // 채팅방 업데이트
        rooms.save(
            room.copy(
                lastUserId = sendUser.id,
                lastMessageId = savedMsg.id,
                updatedId = sendUser.id,
                updatedAt = savedMsg.createdAt
            )
        )

        val sender = users.findSummaryInfoById(sendUser.id)

        val assets = msgAssets.listByMessage(savedMsg.id).map { a ->
            MessageAssetSummary(
                a.uid,
                a.width,
                a.height
            )
        }.toList()

        // unread 계산: 방의 전체 인원 - 읽은 사람 수 - 본인
        val memberCount = chatUsers.countActiveByRoomId(room.id).toInt()
//        val readers = messages.countReaders(savedMsg.id)
//        val unread = ((memberCount - 1) - readers).coerceAtLeast(0)


        // 경험치 부여
        val activeMembers = chatUsers.listActiveUserUids(room.id).toList()
        activeMembers.forEach { member ->
            if (req.kind == 0) {
                expService.grantExp(sendUser.id, "CHAT", member.userId)
            } else if (req.kind == 1) {
                expService.grantExp(sendUser.id, "CHAT_QUICK_EMOJI", member.userId)
            }
        }

        // 푸시 전송
        val pushUserIds = activeMembers.map { it.userId }
        pushService.sendAndSavePush(pushUserIds, PushTemplate.CHAT_MESSAGE.buildPushData("nickname" to sendUser.nickname))

        return MessageSummary.from(
            entity = savedMsg,
            sender = sender,
            assets = assets,
            unreadCount = (memberCount - 1)
        )
    }



    /** 메세지 페이징 조회 */
    suspend fun pageMessages(roomUid: UUID, cursor: OffsetDateTime?, size: Int, userId: Long): List<MessageSummary> {
        val room = rooms.findByUid(roomUid) ?: error("room not found")
        chatUsers.findActive(room.id, userId) ?: error("not a member")

        // 최신 페이지 (DESC)
//        val messagePage = messages.pageByRoom(room.id!!, cursor, size).toList()
        val messagePage = messages.pageByRoomWithEmoji(room.id, cursor, size).toList()
        if (messagePage.isEmpty()) return emptyList()

        val memberCount = chatUsers.countActiveByRoomId(room.id).toInt()

        val minId = messagePage.minOf { it.id}
        val maxId = messagePage.maxOf { it.id}
        val readersMap = messages.countReadersInIdRange(room.id, minId, maxId)
            .toList()
            .associate { it.messageId to it.readerCount }

        return messagePage.map { m ->
            val assets = msgAssets.listByMessage(m.id).map { a ->
                MessageAssetSummary(
                    a.uid,
                    a.width,
                    a.height
                )
            }.toList()

            val sender = users.findSummaryInfoById(m.userId)
            val readers = readersMap[m.id] ?: 0L
            val unread = ((memberCount - 1) - readers).coerceAtLeast(0L).toInt()

            MessageSummary.from(m, sender, assets, unread)
        }
    }


    /** 방 탈퇴 */
    suspend fun leaveRoom(roomUid: UUID, currentUserId: Long) {
        val room = rooms.findByUid(roomUid) ?: error("room not found")
        val me = chatUsers.findActive(room.id, currentUserId) ?: error("not a member")
        chatUsers.save(me.copy(leavingAt = OffsetDateTime.now(), deletedAt = OffsetDateTime.now()))
    }


    /** 푸시 설정 변경 TODO history? */
    suspend fun togglePush(roomUid: UUID, currentUserId: Long, enabled: Boolean) {
        val room = rooms.findByUid(roomUid) ?: error("room not found")
        val me = chatUsers.findActive(room.id, currentUserId) ?: throw BusinessValidationException(mapOf("error" to "not a member"))
        chatUsers.save(me.copy(push = enabled, pushAt = OffsetDateTime.now()))
    }


    suspend fun markRead(roomUid: UUID, currentUser: UserEntity, readMessageUid: UUID) {
        val room = rooms.findByUid(roomUid) ?: error("room not found")
        chatUsers.findActive(room.id, currentUser.id) ?: error("not a member")

        val msg = messages.findByUid(readMessageUid) ?: error("message not found")
        chatUsers.updateReadCursor(room.id, currentUser.id, msg.id, OffsetDateTime.now())

        // ✅ 마지막 메시지 요약
//        val lastMsgEntity = messages.findById(msg.id) ?: return
//        val senderUid = users.findById(lastMsgEntity.userId)?.uid

//        val lastMessageSummary = mapOf(
//            "messageUid" to lastMsgEntity.uid,
//            "userUid" to senderUid,
//            "content" to lastMsgEntity.content,
//            "kind" to lastMsgEntity.kind,
//            "emojiCode" to lastMsgEntity.emojiCode,
//            "createdAt" to lastMsgEntity.createdAt.toString()
//        )

//        val unreadCount = messages.countUnread(room.id, currentUser.id)

//        val payload = mapOf(
//            "roomUid" to room.uid.toString(),
//            "lastMessage" to lastMessageSummary,
//            "unreadCount" to unreadCount.toString()
//        )

//        soketiBroadcaster.broadcast(
//            SoketiChannelPattern.PRIVATE_USER.format(currentUser.uid),
//            SoketiEventType.ROOM_UPDATED,
//            payload
//        )
    }




    /** 목록 스냅샷: cursor + rooms(with unreadCount) */
    suspend fun listRoomsSnapshot(currentUserId: Long, limit: Int, offset: Int): RoomsSnapshotResponse {
        val cursor = messages.findCurrentCursorByUserId(currentUserId)

        val roomsList = rooms.listRooms(currentUserId, limit, offset).map { r ->
            val memberCount = chatUsers.countActiveByRoomId(r.id)

            // 마지막 메시지 + 작성자 조회
            val last = messages.findLastMessage(r.id)
            val sender = last?.userId?.let { uid -> users.findSummaryInfoById(uid) }

            // 마지막 메시지 asset
            val lastAssets = last?.id?.let { msgAssets.listByMessage(it).map { a ->
                MessageAssetSummary(
                    a.uid,
                    a.width,
                    a.height
                )
            }.toList() } ?: emptyList()

            val unreadCount = messages.countUnread(r.id, currentUserId).toInt()

            RoomSummaryResponse(
                roomUid = r.uid,
                roomTitle = r.roomTitle,
                memberCount = memberCount.toInt(),
                unreadCount = unreadCount,
                updatedAt = r.updatedAt ?: r.createdAt,
                lastMessage = if (last != null) {
                    MessageSummary(
                        messageUid = last.uid,
                        content = last.content,
                        kind = last.kind,
                        emojiCode = last.emojiCode,
                        createdAt = last.createdAt,
                        sender = sender,
                        assets = lastAssets
                    )
                } else null,
            )
        }.toList()

        return RoomsSnapshotResponse(cursor = cursor, rooms = roomsList)
    }



}
