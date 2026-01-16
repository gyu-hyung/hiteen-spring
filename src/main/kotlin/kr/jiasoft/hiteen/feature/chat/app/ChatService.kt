package kr.jiasoft.hiteen.feature.chat.app

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kr.jiasoft.hiteen.common.exception.BusinessValidationException
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.asset.domain.AssetCategory
import kr.jiasoft.hiteen.feature.chat.domain.*
import kr.jiasoft.hiteen.feature.chat.dto.*
import kr.jiasoft.hiteen.feature.chat.infra.ChatMessageAssetRepository
import kr.jiasoft.hiteen.feature.chat.infra.ChatMessageRepository
import kr.jiasoft.hiteen.feature.chat.infra.ChatRoomRepository
import kr.jiasoft.hiteen.feature.chat.infra.ChatUserRepository
import kr.jiasoft.hiteen.feature.code.infra.CodeRepository
import kr.jiasoft.hiteen.feature.level.app.ExpService
import kr.jiasoft.hiteen.feature.push.app.PushService
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
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
    private val codeRepository: CodeRepository,
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
    suspend fun createRoom(currentUserId: Long, req: CreateRoomRequest, file: FilePart? = null): UUID {

        val members = users.findAllByUidIn(req.peerUids).toList()
        val peerIds = members.map { it.id }

        val memberIds = (peerIds + currentUserId).distinct()

        // 2명이면 DM
        if (memberIds.size == 2) {
            val otherId = memberIds.first { it != currentUserId }
            val otherUid = users.findUidById(otherId) ?: error("peer not found: $otherId")
            return createDirectRoom(currentUserId, otherUid)
        }

        if (req.reuseExactMembers) {
            rooms.findRoomByExactActiveMembers(memberIds, memberIds.size)?.let { return it.uid }
        }

        // 파일이 있으면 1개만 업로드해서 대표 썸네일로 사용
        val uploadedAssetUid: UUID? = if (file != null) {
            assetService.uploadImages(listOf(file), currentUserId, AssetCategory.COMMON).toList().firstOrNull()?.uid
        } else null


        val saved = rooms.save(
            ChatRoomEntity(
                createdId = currentUserId,
                createdAt = OffsetDateTime.now(),
                roomName = req.roomName ?: members.joinToString(", ") { it.nickname },
                inviteMode = req.inviteMode,
                assetUid = uploadedAssetUid,
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
    suspend fun getRoomByUid(roomUid: UUID): ChatRoomDetailResponse {
        val room = rooms.findByUidAndDeletedAtIsNull(roomUid) ?: error("room not found")
        val members = rooms.listActiveMembersByRoomUid(roomUid).toList()

        val roomRes = ChatRoomResponse(
            id = room.id,
            uid = room.uid,
            lastUserId = room.lastUserId,
            lastMessageId = room.lastMessageId,
            createdId = room.createdId,
            createdAt = room.createdAt,
            updatedId = room.updatedId,
            updatedAt = room.updatedAt,
            deletedId = room.deletedId,
            deletedAt = room.deletedAt,
            roomName = room.roomName!!,
            assetUid = room.assetUid,
            inviteMode = room.inviteMode,
        )

        return ChatRoomDetailResponse(room = roomRes, members = members)
    }


    /** 메시지 전송 */
    suspend fun sendMessage(
        roomUid: UUID,
        sendUser: UserEntity,
        req: SendMessageRequest,
        files: List<FilePart>
    ): MessageSummary {

        val room = rooms.findByUidAndDeletedAtIsNull(roomUid)
            ?: throw IllegalArgumentException("room not found")
        // 방 멤버 확인
        val activeMembers = chatUsers.listActiveUserUids(room.id).toList()
        if (activeMembers.none { it.userId == sendUser.id }) {
            throw IllegalArgumentException("not a member")
        }

        // 메시지 저장
        val savedMsg = messages.save(
            ChatMessageEntity(
                chatRoomId = room.id,
                userId = sendUser.id,
                content = req.content,
                kind = when {
                    req.emojiList != null -> 3
                    files.isNotEmpty() -> 2
                    req.emojiCode != null -> 1
                    else -> 0
                },
                emojiCode = req.emojiList?.first()?.emojiCode ?: req.emojiCode,
                emojiCount = req.emojiList?.first()?.emojiCount ?: req.emojiCount,
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
        val memberCount = activeMembers.count()
//        val readers = messages.countReaders(savedMsg.id)
//        val unread = ((memberCount - 1) - readers).coerceAtLeast(0)


        // 경험치 부여
//        activeMembers.forEach { member ->
//            if (req.kind == 0) {
//                expService.grantExp(sendUser.id, "CHAT", member.userId)
//            } else if (req.kind == 1) {
//                expService.grantExp(sendUser.id, "CHAT_QUICK_EMOJI", member.userId)
//            }
//        }

        // 푸시 전송
        val pushUserIds = activeMembers.filter { it.userId != sender.id  }.map { it.userId }
        val pushMessage = when (savedMsg.kind) {
            0 -> "${sendUser.nickname}: ${req.content}"
            1 -> {
                val emoji = emojiReplace(req.emojiCode!!)
                if (req.emojiCount == null) "${sendUser.nickname}: $emoji"
                else "${sendUser.nickname}: $emoji x${req.emojiCount}"
            }
            2 -> "사진을 보냈습니다."
            3 -> {
                val rows = req.emojiList.orEmpty()
                val uniqueCodes = rows.map { it.emojiCode }.distinct()

                // suspend 호출은 여기(코루틴 본문)에서만 수행
                val emojiMap: Map<String, String> = uniqueCodes.associateWith { code ->
                    emojiReplace(code)
                }

                val emojiSummary = rows
                    .joinToString(" ") { row ->
                        val emoji = emojiMap[row.emojiCode] ?: "[이모티콘]"
                        "$emoji x${row.emojiCount}"
                    }

                "${sendUser.nickname}: $emojiSummary".trim()
            }
            else -> "${sendUser.nickname}: ${req.content}"
        }

        pushService.sendAndSavePush(
            pushUserIds,
            sendUser.id,
            PushTemplate.CHAT_MESSAGE.buildPushData(
                "nickname" to sendUser.nickname,
                        "chat_message" to pushMessage,
            ),
            mapOf("roomUid" to room.uid.toString())
        )

        return MessageSummary.from(
            entity = savedMsg,
            sender = sender,
            assets = assets,
            unreadCount = (memberCount - 1),
            roomUid = room.uid
        )
    }

    private suspend fun emojiReplace(code: String): String {
        // 코드 테이블에서 이모지 코드에 해당하는 값의 col2를 찾아서 반환
        codeRepository.findByGroup("EMOJI").asFlow()
            .firstOrNull { it.code == code }
            ?.let {
                return it.col2 ?: "[이모티콘]"
            }
        return "[이모티콘]"
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

            MessageSummary.from(m, sender, assets, unread, room.uid)
        }
    }


    /** 방 나가기 */
    suspend fun leaveRoom(roomUid: UUID, currentUserId: Long) {
        val room = rooms.findByUid(roomUid) ?: error("room not found")
        val me = chatUsers.findActive(room.id, currentUserId) ?: error("not a member")
        chatUsers.save(me.copy(leavingAt = OffsetDateTime.now(), deletedAt = OffsetDateTime.now()))
        //20251202 1:1 채팅방이면 채팅방 삭제
        if(chatUsers.countActiveByRoomId(room.id) <= 1) {
            rooms.softDeleteById(room.id)
        }
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
            val members = chatUsers.listActiveUserIds(r.id)

            // 마지막 메시지 + 작성자 조회
            val last = messages.findLastMessage(r.id)
            val sender = last?.userId?.let { uid -> users.findSummaryInfoById(uid) }
            //상대방 assetUid
            val otherMemberId = members.firstOrNull { it != currentUserId }
            val otherMember = otherMemberId?.let { users.findSummaryInfoById(it) }

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
                roomTitle = r.roomName,
                memberCount = members.toList().count(),
                unreadCount = unreadCount,
                assetUid = otherMember?.assetUid,
                updatedAt = r.updatedAt ?: r.createdAt,
                lastMessage = if (last != null) {
                    MessageSummary(
                        messageUid = last.uid,
                        roomUid = r.uid,
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


    /**
     * 채팅방 멤버 초대
     * - inviteMode=OWNER: createdId만 초대 가능
     * - inviteMode=ALL_MEMBERS: 방 멤버면 누구나 가능
     * - 초대 시 chat_users upsert로 재참여/중복 초대 처리
     */
    suspend fun inviteMembers(roomUid: UUID, inviterUserId: Long, peerUids: List<UUID>): ChatRoomDetailResponse {
        val room = rooms.findByUidAndDeletedAtIsNull(roomUid) ?: error("room not found")

        // 초대자가 방 멤버인지 체크
        val isInviterMember = chatUsers.existsByRoomUidAndUserId(roomUid, inviterUserId)
        if (!isInviterMember) {
            throw BusinessValidationException(mapOf("error" to "not a member"))
        }

        // invite_mode 권한 체크
        if (room.inviteMode == ChatRoomInviteMode.OWNER && room.createdId != inviterUserId) {
            throw BusinessValidationException(mapOf("error" to "invite forbidden"))
        }

        val distinctPeerUids = peerUids.distinct()
        if (distinctPeerUids.isEmpty()) return getRoomByUid(roomUid)

        // UID -> userId 변환 (없는 유저면 예외)
        val inviteeIds = distinctPeerUids.map { uid ->
            users.findByUid(uid.toString())?.id
                ?: throw BusinessValidationException(mapOf("peerUid" to "not found: $uid"))
        }

        // 본인 제외 + 중복 제거
        val filteredInviteeIds = inviteeIds.filter { it != inviterUserId }.distinct()
        if (filteredInviteeIds.isEmpty()) return getRoomByUid(roomUid)

        // 이미 활성 멤버는 제외
        val existingActiveIds = chatUsers.listActiveUserIds(room.id).toList().toSet()
        val now = OffsetDateTime.now()

        filteredInviteeIds
            .filter { it !in existingActiveIds }
            .forEach { inviteeId ->
                chatUsers.upsertRejoin(
                    chatRoomId = room.id,
                    userId = inviteeId,
                    joiningAt = now,
                    pushAt = now,
                )
            }

        return getRoomByUid(roomUid)
    }

    /**
     * 채팅방 수정
     * - createRoom(단톡 생성) 정책과 동일하게 처리
     *   1) file(1개) 첨부 시 업로드한 파일이 assetUid를 덮어씀
     *   2) file 없으면 req.assetUid가 있으면 그것을 사용
     *   3) 둘 다 없으면 기존 assetUid 유지
     * - 권한: 방 생성자(createdId)만 수정 가능(안전 기본값)
     */
    suspend fun updateRoom(roomUid: UUID, currentUserId: Long, req: UpdateRoomRequest, file: FilePart? = null) {
        val room = rooms.findByUidAndDeletedAtIsNull(roomUid) ?: error("room not found")

        if (room.createdId != currentUserId) {
            throw BusinessValidationException(mapOf("error" to "forbidden"))
        }

        val uploadedAssetUid: UUID? = if (file != null) {
            assetService.uploadImages(listOf(file), currentUserId, AssetCategory.COMMON).toList().firstOrNull()?.uid
        } else null

        val newAssetUid = uploadedAssetUid ?: (req.assetUid ?: room.assetUid)

        rooms.save(
            room.copy(
                roomName = req.roomName ?: room.roomName,
                inviteMode = req.inviteMode ?: room.inviteMode,
                assetUid = newAssetUid,
                updatedId = currentUserId,
                updatedAt = OffsetDateTime.now(),
            )
        )
    }

}
