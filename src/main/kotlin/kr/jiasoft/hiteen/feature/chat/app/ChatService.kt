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
import kr.jiasoft.hiteen.feature.push.app.event.PushSendRequestedEvent
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.dto.UserSummary
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import org.springframework.context.ApplicationEventPublisher
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
    private val assetService: AssetService,
    private val eventPublisher: ApplicationEventPublisher,
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

        val now = OffsetDateTime.now()

        val saved = rooms.save(
            ChatRoomEntity(
                createdId = currentUserId,
                createdAt = now,
                updatedAt = now
            )
        )
        chatUsers.save(ChatUserEntity(chatRoomId = saved.id, userId = currentUserId, push = true, pushAt = now, joiningAt = now))
        chatUsers.save(ChatUserEntity(chatRoomId = saved.id, userId = peer.id, push = true, pushAt = now, joiningAt = now))

        // --- ✅ 입장 시스템 메시지 생성 및 저장 (kind 4) ---
        val systemContent = "채팅방에 입장하였습니다."

        val systemMsg = messages.save(
            ChatMessageEntity(
                chatRoomId = saved.id,
                userId = saved.createdId, // 방장(owner)을 발송자로 저장(leaveRoom과 동일)
                content = systemContent,
                kind = 4,
                createdAt = now,
            )
        )

        rooms.save(
            saved.copy(
                lastUserId = saved.createdId,
                lastMessageId = systemMsg.id,
                updatedId = currentUserId,
                updatedAt = systemMsg.createdAt,
            )
        )

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

        val now = OffsetDateTime.now()

        // 파일이 있으면 1개만 업로드해서 대표 썸네일로 사용
        val uploadedAssetUid: UUID? = if (file != null) {
            assetService.uploadImages(listOf(file), currentUserId, AssetCategory.COMMON).toList().firstOrNull()?.uid
        } else null


        val saved = rooms.save(
            ChatRoomEntity(
                createdId = currentUserId,
                createdAt = now,
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
                    pushAt = now,
                    joiningAt = now
                )
            )
        }

        // --- ✅ 입장 시스템 메시지 생성 및 저장 (kind 4) ---
        val systemContent = "채팅방에 입장하였습니다."

        val systemMsg = messages.save(
            ChatMessageEntity(
                chatRoomId = saved.id,
                userId = saved.createdId, // 방장(owner)을 발송자로 저장(leaveRoom과 동일)
                content = systemContent,
                kind = 4,
                createdAt = now,
            )
        )

        rooms.save(
            saved.copy(
                lastUserId = saved.createdId,
                lastMessageId = systemMsg.id,
                updatedId = currentUserId,
                updatedAt = systemMsg.createdAt,
            )
        )

        return saved.uid
    }


    /** 방 조회 TODO 상세 구현(메세지 목록?, 참여 멤버?)*/
    suspend fun getRoomByUid(roomUid: UUID, systemMessage: MessageSummary? = null): ChatRoomDetailResponse {
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

        return ChatRoomDetailResponse(room = roomRes, members = members, systemMessage = systemMessage)
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

        // ✅ kind=3(emojiList)일 때 DB에 저장할 content를 '♥️ x100 💩 x100 ...' 형태로 구성
        val emojiListContent: String? = req.emojiList?.let { rows ->
            val uniqueCodes = rows.map { it.emojiCode }.distinct()
            val emojiMap: Map<String, String> = uniqueCodes.associateWith { code ->
                emojiReplace(code)
            }
            rows.joinToString(" ") { row ->
                val emoji = emojiMap[row.emojiCode] ?: "[이모티콘]"
                "$emoji x${row.emojiCount}"
            }
        }

        // 메시지 저장
        val savedMsg = messages.save(
            ChatMessageEntity(
                chatRoomId = room.id,
                userId = sendUser.id,
                content = emojiListContent ?: req.content,
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
                val emojiSummary = emojiListContent ?: ""
                "${sendUser.nickname}: $emojiSummary".trim()
            }
            else -> "${sendUser.nickname}: ${req.content}"
        }

        eventPublisher.publishEvent(
            PushSendRequestedEvent(
                userIds = pushUserIds,
                actorUserId = sendUser.id,
                templateData = PushTemplate.CHAT_MESSAGE.buildPushData(
                    "nickname" to sendUser.nickname,
                    "chat_message" to pushMessage,
                ),
                extraData = mapOf("roomUid" to room.uid.toString()),
            )
        )

        return MessageSummary.from(
            entity = savedMsg,
            sender = sender,
            assets = assets,
            unreadCount = (memberCount - 1),
            roomUid = room.uid,
            emojiList = req.emojiList
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

        // 최신 페이지 조회 (최적화 버전)
        val projections = messages.pageMessagesSummary(room.id, cursor, size).toList()
        if (projections.isEmpty()) return emptyList()

        // 메시지 id 목록 수집
        val msgIds = projections.map { it.id }

        // 에셋 일괄 조회 (N+1 방지)
        val assetsMap = msgAssets.findAllByMessageIdIn(msgIds).toList()
            .groupBy { it.messageId }
            .mapValues { (_, assets) ->
                assets.map { a -> MessageAssetSummary(a.uid, a.width, a.height) }
            }

        // 방 참여 멤버들의 마지막 읽은 메시지 ID 정보 일괄 조회 (N+1 최적화)
        val members = chatUsers.listByRoom(room.id).toList()
        val memberCount = members.size

        return projections.map { p ->
            // Kotlin 레벨에서 읽음 수 계산 (DB 상관 서브쿼리 제거로 속도 개선)
            val readerCount = members.count { m ->
                m.userId != p.userId && (m.lastReadMessageId ?: 0) >= p.id
            }

            MessageSummary(
                messageUid = p.messageUid,
                roomUid = room.uid,
                content = p.content,
                kind = p.kind,
                emojiCode = p.emojiCode,
                emojiCount = p.emojiCount,
                createdAt = p.createdAt,
                sender = UserSummary(
                    id = p.senderId,
                    uid = p.senderUid.toString(),
                    username = p.senderUsername,
                    nickname = p.senderNickname,
                    address = null,
                    detailAddress = null,
                    phone = null,
                    mood = null,
                    moodEmoji = null,
                    mbti = null,
                    expPoints = 0,
                    tierId = 0,
                    tierName = "",
                    assetUid = p.senderAssetUid,
                    gender = null,
                    isFriend = null,
                    isFriendRequest = null
                ),
                assets = assetsMap[p.id] ?: emptyList(),
                unreadCount = (memberCount - 1 - readerCount).coerceAtLeast(0)
            )
        }
    }


    /** 방 나가기 */
    suspend fun leaveRoom(roomUid: UUID, currentUserId: Long): MessageSummary? {
        val room = rooms.findByUid(roomUid) ?: error("room not found")
        val me = chatUsers.findActive(room.id, currentUserId) ?: error("not a member")
        val leavingUser = users.findById(currentUserId) ?: error("user not found")

        chatUsers.save(me.copy(leavingAt = OffsetDateTime.now(), deletedAt = OffsetDateTime.now()))

        // 20251202 1:1 채팅방이면 채팅방 삭제
        val remainingCount = chatUsers.countActiveByRoomId(room.id)
        if(remainingCount < 1) {
            rooms.softDeleteById(room.id)
            return null
        }

        // --- ✅ 퇴장 시스템 메시지 생성 및 저장 (kind 4) ---
        val now = OffsetDateTime.now()
        val ownerId = room.createdId
        val systemContent = "${leavingUser.nickname}님이 나갔습니다."

        val savedMsg = messages.save(
            ChatMessageEntity(
                chatRoomId = room.id,
                userId = ownerId, // 방장(owner)을 발송자로 저장
                content = systemContent,
                kind = 4, // 시스템 메세지
                createdAt = now,
            )
        )

        // 채팅방 업데이트 (마지막 메시지 갱신)
        rooms.save(
            room.copy(
                lastUserId = ownerId,
                lastMessageId = savedMsg.id,
                updatedId = currentUserId,
                updatedAt = savedMsg.createdAt
            )
        )

        return MessageSummary.from(
            entity = savedMsg,
            sender = users.findSummaryInfoById(ownerId),
            assets = emptyList(),
            roomUid = room.uid,
            unreadCount = remainingCount.toInt()
        )
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
        val userId = currentUserId
        val cursor = messages.findCurrentCursorByUserId(userId)

        val projections = rooms.listRoomSummaries(userId, limit, offset).toList()
        if (projections.isEmpty()) return RoomsSnapshotResponse(cursor = cursor, rooms = emptyList())

        val roomIds = projections.map { it.id }

        // 1) 각 방의 안읽은 메시지 수 일괄 조회
        val unreadMap: Map<Long, Int> = messages.countUnreadByRoomIds(roomIds, userId).toList()
            .associate { it.messageId to it.readerCount.toInt() }

        // 2) 각 방의 멤버 정보 일괄 조회 (멤버 수 및 제목 생성용, 방 나간 유저 포함)
        val membersGroupByRoom: Map<Long, List<ChatUserNicknameProjection>> = chatUsers.findAllDetailedByRoomIds(roomIds).toList()
            .groupBy { it.chatRoomId }

        // 3) 마지막 메시지용 에셋 일괄 조회
        val lastMsgIds = projections.mapNotNull { it.lastMessageId }
        val assetsMap: Map<Long, List<MessageAssetSummary>> = if (lastMsgIds.isNotEmpty()) {
            msgAssets.findAllByMessageIdIn(lastMsgIds).toList()
                .groupBy { it.messageId }
                .mapValues { (_, assets) ->
                    assets.map { a -> MessageAssetSummary(a.uid, a.width, a.height) }
                }
        } else emptyMap()

        val roomsList = projections.map { p ->
            val roomMembers = membersGroupByRoom[p.id] ?: emptyList()

            // 방 제목 생성 (room_name이 없으면 참여자 닉네임 조합)
            val computedTitle = if (!p.roomTitle.isNullOrBlank()) {
                p.roomTitle
            } else {
                //단톡일때
                if( roomMembers.size < 2 ) {
                    roomMembers.filter { it.userId != userId }
                        .take(3) // 최대 3명까지 노출
                        .joinToString(", ") { it.nickname }
                        .let { nicknames ->
                            if (roomMembers.size > 4) "$nicknames 외 ${roomMembers.size - 4}명"
                            else nicknames
                        }
                } else {//갠톡일때
                    roomMembers.firstOrNull { it.userId != userId }?.nickname ?: "알 수 없음"
                }
            }

            val lastMsgSummary = p.lastMessageId?.let { lid ->
                MessageSummary(
                    messageUid = p.lastMessageUid!!,
                    roomUid = p.roomUid,
                    content = p.lastContent,
                    kind = p.lastKind ?: 0,
                    emojiCode = p.lastEmojiCode,
                    emojiCount = p.lastEmojiCount,
                    createdAt = p.lastCreatedAt!!,
                    sender = if (p.lastSenderId != null) {
                        UserSummary(
                            id = p.lastSenderId,
                            uid = p.lastSenderUid.toString(),
                            username = p.lastSenderUsername ?: "",
                            nickname = p.lastSenderNickname,
                            address = null,
                            detailAddress = null,
                            phone = null,
                            mood = null,
                            moodEmoji = null,
                            mbti = null,
                            expPoints = 0,
                            tierId = 0,
                            tierName = "",
                            assetUid = p.lastSenderAssetUid,
                            gender = null,
                            isFriend = null,
                            isFriendRequest = null
                        )
                    } else null,
                    assets = assetsMap[lid] ?: emptyList()
                )
            }

            RoomSummaryResponse(
                roomUid = p.roomUid,
                roomTitle = computedTitle,
                memberCount = roomMembers.size,
                unreadCount = unreadMap[p.id] ?: 0,
                assetUid = p.assetUid,
                updatedAt = p.updatedAt,
                lastMessage = lastMsgSummary
            )
        }

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

        // UID -> userId 변환 (N+1 방지: 일괄 조회)
        val invitees = users.findAllByUidIn(distinctPeerUids)
        if (invitees.size != distinctPeerUids.size) {
            val foundUids = invitees.map { it.uid.toString() }.toSet()
            val missingUid = distinctPeerUids.find { it.toString() !in foundUids }
            throw BusinessValidationException(mapOf("peerUid" to "not found: $missingUid"))
        }

        // 본인 제외 + 중복 제거
        val filteredInvitees = invitees.filter { it.id != inviterUserId }.distinctBy { it.id }
        if (filteredInvitees.isEmpty()) return getRoomByUid(roomUid)

        // 이미 활성 멤버는 제외
        val existingActiveIds = chatUsers.listActiveUserIds(room.id).toList().toSet()
        val reallyNewInvitees = filteredInvitees.filter { it.id !in existingActiveIds }
        if (reallyNewInvitees.isEmpty()) return getRoomByUid(roomUid)

        val now = OffsetDateTime.now()

        reallyNewInvitees.forEach { invitee ->
            chatUsers.upsertRejoin(
                chatRoomId = room.id,
                userId = invitee.id,
                joiningAt = now,
                pushAt = now,
            )
        }

        // --- ✅ 초대 시스템 메시지 생성 및 저장 (kind 4) ---
        val inviter = users.findById(inviterUserId) ?: error("inviter not found")
        val ownerId = room.createdId // 방장 ID
        val inviteeNames = reallyNewInvitees.joinToString(", ") { it.nickname }
        val systemContent = "${inviter.nickname}님이 ${inviteeNames}님을 초대했습니다."

        val savedMsg = messages.save(
            ChatMessageEntity(
                chatRoomId = room.id,
                userId = ownerId, // 방장(owner)을 발송자로 저장
                content = systemContent,
                kind = 4, // 시스템 메세지
                createdAt = now,
            )
        )

        // 채팅방 업데이트 (마지막 메시지 갱신)
        rooms.save(
            room.copy(
                lastUserId = ownerId,
                lastMessageId = savedMsg.id,
                updatedId = inviterUserId,
                updatedAt = savedMsg.createdAt
            )
        )

        val systemMsgSummary = MessageSummary.from(
            entity = savedMsg,
            sender = users.findSummaryInfoById(ownerId),
            assets = emptyList(),
            roomUid = room.uid,
            unreadCount = (chatUsers.countActiveByRoomId(room.id).toInt() - 1)
        )

        return getRoomByUid(roomUid, systemMsgSummary)
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
