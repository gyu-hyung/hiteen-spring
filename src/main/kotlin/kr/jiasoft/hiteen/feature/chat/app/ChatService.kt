package kr.jiasoft.hiteen.feature.chat.app

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
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
import kr.jiasoft.hiteen.feature.soketi.app.SoketiBroadcaster
import kr.jiasoft.hiteen.feature.soketi.domain.SoketiChannelPattern
import kr.jiasoft.hiteen.feature.soketi.domain.SoketiEventType
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.util.UUID

@Service
class ChatService(
    private val rooms: ChatRoomRepository,
    private val chatUsers: ChatUserRepository,
    private val messages: ChatMessageRepository,
    private val msgAssets: ChatMessageAssetRepository,
    private val users: UserRepository,
    private val soketiBroadcaster: SoketiBroadcaster,

) {

    /** DM 방 생성 TODO 친구가 맞는지? */
    suspend fun createDirectRoom(currentUserId: Long, peerUid: UUID): UUID {
        val peer = users.findByUid(peerUid.toString()) ?: error("peer not found")
        val existing = rooms.findDirectRoom(currentUserId, peer.id!!)
        if (existing != null) return existing.uid

        val saved = rooms.save(
            ChatRoomEntity(
                createdId = currentUserId,
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now()
            )
        )
        chatUsers.save(ChatUserEntity(chatRoomId = saved.id!!, userId = currentUserId, push = true, pushAt = OffsetDateTime.now(), joiningAt = OffsetDateTime.now()))
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
                updatedAt = OffsetDateTime.now()
            )
        )

        // 멤버 추가
        memberIds.forEach { uid ->
            chatUsers.save(
                ChatUserEntity(
                    chatRoomId = saved.id!!,
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
    suspend fun sendMessage(roomUid: UUID, sendUser: UserEntity, req: SendMessageRequest): UUID {
        val room = rooms.findByUidAndDeletedAtIsNull(roomUid) ?: error("room not found")
        chatUsers.findActive(room.id!!, sendUser.id!!) ?: error("not a member")

        // 메시지 저장
        val savedMsg = messages.save(
            ChatMessageEntity(
                chatRoomId = room.id,
                userId = sendUser.id,
                content = req.content,
                createdAt = OffsetDateTime.now(),
                kind = req.kind,
                emojiCode = req.emojiCode,
            )
        )

        // 첨부 파일 저장
        req.assetUids?.forEach { au ->
            msgAssets.save(ChatMessageAssetEntity(uid = au, messageId = savedMsg.id!!))
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

        // 마지막 메시지 요약
        val lastMessageSummary = mapOf(
            "messageUid" to savedMsg.uid,
            "userUid" to sendUser.uid,
            "content" to req.content,
            "kind" to req.kind,
            "emojiCode" to req.emojiCode,
            "createdAt" to savedMsg.createdAt.toString()
        )

        // 채팅방 채널 broadcast (message-created)
        val messagePayload = mapOf(
            "roomUid" to room.uid.toString(),
            "message" to lastMessageSummary
        )
        soketiBroadcaster.broadcast(
            SoketiChannelPattern.PRIVATE_CHAT_ROOM.format(room.uid),
            SoketiEventType.MESSAGE_CREATED,
            messagePayload
        )

        // 채팅방 멤버들에게 broadcast (ROOM_UPDATED + unreadCount)
        val activeMembers = chatUsers.listActiveUserUids(room.id).toList()
        for (member in activeMembers) {

            val unreadCount = messages.countUnread(room.id, member.userId)

            val payload = mapOf(
                "roomUid" to room.uid.toString(),
                "lastMessage" to lastMessageSummary,
                "unreadCount" to unreadCount.toString()
            )

            soketiBroadcaster.broadcast(
                SoketiChannelPattern.PRIVATE_USER.format(member.userUid),
                SoketiEventType.ROOM_UPDATED,
                payload
            )
        }

        return savedMsg.uid
    }




    /** 메세지 페이징 조회 */
    suspend fun pageMessages(roomUid: UUID, cursor: OffsetDateTime?, size: Int): List<MessageSummary> {
        val room = rooms.findByUid(roomUid) ?: error("room not found")

        // 최신 페이지 (DESC)
//        val messagePage = messages.pageByRoom(room.id!!, cursor, size).toList()
        val messagePage = messages.pageByRoomWithEmoji(room.id!!, cursor, size).toList()
        if (messagePage.isEmpty()) return emptyList()

        val memberCount = chatUsers.countActiveByRoom(room.id).toInt()

        val minId = messagePage.minOf { it.id!! }
        val maxId = messagePage.maxOf { it.id!! }
        val readersMap = messages.countReadersInIdRange(room.id, minId, maxId)
            .toList()
            .associate { it.messageId to it.readerCount }

        return messagePage.map { m ->
            val assets = msgAssets.listByMessage(m.id!!).map { a ->
                MessageAssetSummary(
//                    a.id,
                    a.uid,
//                    m.id ,
                    a.width,
                    a.height
                )
            }.toList()

            val senderUid: UUID = users.findById(m.userId)?.uid
                ?: throw IllegalStateException("sender user not found: ${m.userId}")

            val readers = readersMap[m.id] ?: 0L
            val unread = ((memberCount - 1) - readers).coerceAtLeast(0L).toInt()

            MessageSummary(
                messageUid = m.uid,
                senderUid = senderUid,
                content = m.content,
                createdAt = m.createdAt,
                assets = assets,
                unreadCount = unread
            )
        }
    }


    /** 방 탈퇴 */
    suspend fun leaveRoom(roomUid: UUID, currentUserId: Long) {
        val room = rooms.findByUid(roomUid) ?: error("room not found")
        val me = chatUsers.findActive(room.id!!, currentUserId) ?: return
        chatUsers.save(me.copy(leavingAt = OffsetDateTime.now(), deletedAt = OffsetDateTime.now()))
    }


    /** 푸시 설정 변경 TODO history? */
    suspend fun togglePush(roomUid: UUID, currentUserId: Long, enabled: Boolean) {
        val room = rooms.findByUid(roomUid) ?: error("room not found")
        val me = chatUsers.findActive(room.id!!, currentUserId) ?: error("not a member")
        chatUsers.save(me.copy(push = enabled, pushAt = OffsetDateTime.now()))
    }


    suspend fun assertMember(roomUid: UUID, userId: Long) {
        val room = rooms.findByUid(roomUid) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "room not found")
        val me = chatUsers.findActive(room.id!!, userId)
        if (me == null) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "not a member")
        }
    }


    suspend fun markRead(roomUid: UUID, currentUser: UserEntity, lastMessageUid: UUID) {
        val room = rooms.findByUid(roomUid)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "room not found")
        chatUsers.findActive(room.id!!, currentUser.id!!)
            ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "not a member")

        val msg = messages.findByUid(lastMessageUid) ?: return
        chatUsers.updateReadCursor(room.id, currentUser.id, msg.id!!, OffsetDateTime.now())

        // ✅ 마지막 메시지 요약
        val lastMsgEntity = messages.findById(msg.id) ?: return
        val senderUid = users.findById(lastMsgEntity.userId)?.uid

        val lastMessageSummary = mapOf(
            "messageUid" to lastMsgEntity.uid,
            "userUid" to senderUid,
            "content" to lastMsgEntity.content,
            "kind" to lastMsgEntity.kind,
            "emojiCode" to lastMsgEntity.emojiCode,
            "createdAt" to lastMsgEntity.createdAt.toString()
        )

        val unreadCount = messages.countUnread(room.id, currentUser.id)

        val payload = mapOf(
            "roomUid" to room.uid.toString(),
            "lastMessage" to lastMessageSummary,
            "unreadCount" to unreadCount.toString()
        )

        soketiBroadcaster.broadcast(
            SoketiChannelPattern.PRIVATE_USER.format(currentUser.uid),
            SoketiEventType.ROOM_UPDATED,
            payload
        )
    }




    /** 목록 스냅샷: cursor + rooms(with unreadCount) */
    suspend fun listRoomsSnapshot(currentUserId: Long, limit: Int, offset: Int): RoomsSnapshotResponse {
        val cursor = messages.findCurrentCursorByUserId(currentUserId)

        val roomsList = rooms.listRooms(currentUserId, limit, offset).map { r ->
            //멤버수
            val memberCount = chatUsers.countActiveByRoom(r.id!!)
            //마지막 메세지
            val last = messages.findLastMessage(r.id)
            //마지막 메세지의 asset
            val lastAssets = last?.id?.let { msgAssets.listByMessage(it).map { a ->
                MessageAssetSummary(
//                    a.id,
                    a.uid,
//                    m.id ,
                    a.width,
                    a.height
                )
            }.toList() } ?: emptyList()
            //읽지 않은 메세지 수
            val unreadCount = messages.countUnread(r.id, currentUserId).toInt()

            RoomSummaryResponse(
                roomUid = r.uid,
                lastMessage = last?.let { lm ->
                    //TODO 성능
                    val senderUid: UUID = users.findById(lm.userId)?.uid
                        ?: throw IllegalStateException("sender user not found: ${lm.userId}")
                    MessageSummary(
                        messageUid = lm.uid,
                        senderUid = senderUid,
                        content = lm.content,
                        createdAt = lm.createdAt,
                        assets = lastAssets,
                    )
                },
                memberCount = memberCount.toInt(),
                unreadCount = unreadCount,
                updatedAt = r.updatedAt ?: r.createdAt
            )

        }.toList()

        return RoomsSnapshotResponse(cursor = cursor, rooms = roomsList)
    }


}
