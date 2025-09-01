package kr.jiasoft.hiteen.feature.chat.app

import com.fasterxml.jackson.databind.ObjectMapper
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
    private val inbox: InboxHub,
    private val mapper: ObjectMapper,
) {

    /** DM 방 생성 */
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


    /** 방 조회 */
    suspend fun getRoomByUid(roomUid: UUID): ChatRoomEntity =
        rooms.findByUid(roomUid) ?: error("room not found")


    /** 메시지 전송 */
    suspend fun sendMessage(roomUid: UUID, currentUserId: Long, req: SendMessageRequest): UUID {
        val room = rooms.findByUid(roomUid) ?: error("room not found")
        chatUsers.findActive(room.id!!, currentUserId) ?: error("not a member")

        val savedMsg = messages.save(
            ChatMessageEntity(
                chatRoomId = room.id,
                userId = currentUserId,
                content = req.content,
                createdAt = OffsetDateTime.now()
            )
        )
        req.assetUids?.forEach { au ->
            msgAssets.save(ChatMessageAssetEntity(messageId = savedMsg.id!!, assetUid = au))
        }
        rooms.save(room.copy(
            lastUserId = currentUserId, lastMessageId = savedMsg.id,
            updatedId = currentUserId, updatedAt = savedMsg.createdAt
        ))

        // ★ sender userUid 조회
        val senderUid = users.findById(currentUserId)?.uid
            ?: throw IllegalStateException("sender not found: $currentUserId")

        // ★ 목록 실시간 델타 발행: userUid로 교체
        val lastSummary = mapOf(
            "uid" to savedMsg.uid,
            "userUid" to senderUid,
            "content" to req.content,
            "createdAt" to savedMsg.createdAt
        )

        // 방 멤버 조회
        val memberIds = chatUsers.listActiveUserIds(room.id).toList()
        for (memberId in memberIds) {
            val unread = chatUsers.countUnread(room.id, memberId).toInt()
            val payload = mapper.writeValueAsString(
                mapOf(
                    "type" to "room-updated",
                    "cursor" to (savedMsg.id ?: 0L),
                    "roomUid" to room.uid.toString(),
                    "lastMessage" to lastSummary,
                    "unreadCount" to unread
                )
            )
            inbox.publishTo(memberId, payload)
        }
        return savedMsg.uid
    }


    /** 메세지 페이징 조회 */
    suspend fun pageMessages(roomUid: UUID, cursor: OffsetDateTime?, size: Int): List<MessageSummary> {
        val room = rooms.findByUid(roomUid) ?: error("room not found")

        // 최신 페이지 (DESC)
        val messagePage = messages.pageByRoom(room.id!!, cursor, size).toList()
        if (messagePage.isEmpty()) return emptyList()

        val memberCount = chatUsers.countActiveByRoom(room.id).toInt()

        val minId = messagePage.minOf { it.id!! }
        val maxId = messagePage.maxOf { it.id!! }
        val readersMap = chatUsers.countReadersInIdRange(room.id, minId, maxId)
            .toList()
            .associate { it.messageId to it.readerCount }

        return messagePage.map { m ->
            val assets = msgAssets.listByMessage(m.id!!).map { a ->
                MessageAssetSummary(a.uid, a.assetUid, a.width, a.height)
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

    suspend fun markRead(roomUid: UUID, userId: Long, lastMessageUid: UUID) {
        val room = rooms.findByUid(roomUid) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "room not found")
        chatUsers.findActive(room.id!!, userId) ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "not a member")

        val msg = messages.findByUid(lastMessageUid) ?: return
        chatUsers.updateReadCursor(room.id, userId, msg.id!!, OffsetDateTime.now())

        val last = messages.findLastMessage(room.id)

        // ★ lastMessage.userUid로 변환
        val lastSummary = last?.let {
            val lastSenderUid = users.findById(it.userId)?.uid
                ?: throw IllegalStateException("sender user not found: ${it.userId}")
            mapOf(
                "uid" to it.uid,
                "userUid" to lastSenderUid,
                "content" to it.content,
                "createdAt" to it.createdAt
            )
        } ?: mapOf<String, Any>()

        val unreadNow = chatUsers.countUnread(room.id, userId).toInt()
        val payload = mapper.writeValueAsString(
            mapOf(
                "type" to "room-updated",
                "cursor" to (last?.id ?: msg.id),
                "roomUid" to room.uid.toString(),
                "lastMessage" to lastSummary,
                "unreadCount" to unreadNow
            )
        )
        inbox.publishTo(userId, payload)
    }



    /** 목록 스냅샷: cursor + rooms(with unreadCount) */
    suspend fun listRoomsSnapshot(currentUserId: Long, limit: Int, offset: Int): RoomsSnapshotResponse {
        val cursor = messages.currentCursorForUser(currentUserId)

        val roomsList = rooms.listRooms(currentUserId, limit, offset).map { r ->
            val memberCount = chatUsers.countActiveByRoom(r.id!!)
            val last = messages.findLastMessage(r.id)//TODO 성능
            val lastAssets = last?.id?.let { msgAssets.listByMessage(it).map { a ->
                MessageAssetSummary(a.uid, a.assetUid, a.width, a.height)
            }.toList() } ?: emptyList()

            val unread = chatUsers.countUnread(r.id, currentUserId).toInt()

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
                unreadCount = unread,
                updatedAt = r.updatedAt ?: r.createdAt
            )

        }.toList()

        return RoomsSnapshotResponse(cursor = cursor, rooms = roomsList)
    }


}
