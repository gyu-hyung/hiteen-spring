package kr.jiasoft.hiteen.feature.relationship.app

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import kr.jiasoft.hiteen.common.exception.BusinessValidationException
import kr.jiasoft.hiteen.feature.contact.infra.UserContactBulkRepository
import kr.jiasoft.hiteen.feature.level.app.ExpService
import kr.jiasoft.hiteen.feature.location.domain.LocationHistory
import kr.jiasoft.hiteen.feature.location.infra.cache.LocationCacheRedisService
import kr.jiasoft.hiteen.feature.push.app.event.PushSendRequestedEvent
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
import kr.jiasoft.hiteen.feature.relationship.RelationHistoryService
import kr.jiasoft.hiteen.feature.relationship.domain.*
import kr.jiasoft.hiteen.feature.relationship.dto.*
import kr.jiasoft.hiteen.feature.relationship.infra.FollowRepository
import kr.jiasoft.hiteen.feature.relationship.infra.FriendRepository
import kr.jiasoft.hiteen.feature.user.app.UserService
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.dto.UserResponse
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class FriendService(
    private val friendRepository: FriendRepository,
    private val followRepository: FollowRepository,
    private val relationHistoryService: RelationHistoryService,

    private val userRepository: UserRepository,
//    private val userContactRepository: UserContactRepository,
    private val userContactBulkRepository: UserContactBulkRepository,

    private val userService: UserService,
    private val locationCacheRedisService: LocationCacheRedisService,
    private val expService: ExpService,

    private val eventPublisher: ApplicationEventPublisher,
) {
    private val now: OffsetDateTime get() = OffsetDateTime.now(ZoneOffset.UTC)

    private suspend fun requireUserIdByUid(uid: String): Long {
        return userRepository.findByUid(uid)?.id
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "user not found: $uid")
    }

    private fun toRelationshipSummary(
            e: FriendEntity,
            myId: Long,
            other: UserResponse,
            latestLocation: LocationHistory?
    ): RelationshipSummary {
        val myMode = if (e.userId == myId) e.userLocationMode else e.friendLocationMode
        val theirMode = if (e.userId == myId) e.friendLocationMode else e.userLocationMode

        return RelationshipSummary(
            userResponse = other,
            status = e.status,
            statusAt = e.statusAt,
            lat = latestLocation?.lat,
            lng = latestLocation?.lng,
            lastSeenAt = latestLocation?.timestamp?.let {
                Instant.ofEpochMilli(it).atOffset(ZoneOffset.ofHours(9))
            },
            myLocationMode = myMode,
            theirLocationMode = theirMode
        )
    }


    suspend fun findUserByUid(uid: String): UserEntity? {
        return userRepository.findByUid(uid)
    }


    suspend fun getContacts(user: UserEntity, rawContacts: String): ContactResponse {
        return getContactsInternal(userId = user.id, rawContacts = rawContacts)
    }

    /**
     * 동기/비동기(잡) 공용 로직
     */
    internal suspend fun getContactsInternal(userId: Long, rawContacts: String): ContactResponse {
        // 1) 연락처 문자열 -> 전화번호 dedupe
        // split은 1천~수만 라인에서도 충분히 동작하지만, 긴 문자열에서 trim/필터를 최소화
        val phones = rawContacts
            .lineSequence()
            .map { line ->
                buildString {
                    line.forEach { ch -> if (ch.isDigit()) append(ch) }
                }
            }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()

        if (phones.isEmpty()) {
            throw BusinessValidationException(mapOf("message" to "연락처 정보가 없습니다."))
        }

        // 2) DB에 연락처 저장 (bulk upsert)
        // per-row upsert(1000번 왕복) 대신 1번 쿼리로 처리
        userContactBulkRepository.upsertAllPhones(userId, phones.toList())

        // 3) 가입 사용자 조회
        val registeredUsers = userRepository.findAllUserSummaryByPhoneIn(phones, userId).toList()

        // 4) 친구 관계 조회
        val friendIds = friendRepository.findAllFriendship(userId).toSet()

        // 5) 그룹 분류
        val friendList = registeredUsers.filter { it.id in friendIds }
        val registeredAndNotFriend = registeredUsers.filter { it.id !in friendIds && it.id != userId }
        val notRegistered = phones.filter { phone -> registeredUsers.none { it.phone == phone } }

        return ContactResponse(registeredAndNotFriend, friendList, notRegistered)
    }


    suspend fun listFriends(me: UserEntity): List<RelationshipSummary> {
        val friends = friendRepository.findAllAccepted(me.id).toList()
        if (friends.isEmpty()) return emptyList()

        val otherIds = friends.map { if (it.userId == me.id) it.friendId else it.userId }
        val userMap = userService.findUserResponseByIds(otherIds, me.id).associateBy { it.id }

        return friends.mapNotNull { e ->
            val otherId = if (e.userId == me.id) e.friendId else e.userId
            userMap[otherId]?.let { other ->
                val latestLocation = locationCacheRedisService.getLatest(other.uid)
                toRelationshipSummary(e, me.id, other, latestLocation)
            }
        }
    }


    suspend fun listOutgoing(me: UserEntity): List<RelationshipSummary> {
        val friends = friendRepository.findAllOutgoingPending(me.id).toList()
        if (friends.isEmpty()) return emptyList()

        val otherIds = friends.map { if (it.userId == me.id) it.friendId else it.userId }
        val userMap = userService.findUserResponseByIds(otherIds, me.id).associateBy { it.id }

        return friends.mapNotNull { e ->
            val otherId = if (e.userId == me.id) e.friendId else e.userId
            userMap[otherId]?.let { other ->
                toRelationshipSummary(e, me.id, other, null)
            }
        }
    }


    suspend fun listIncoming(me: UserEntity): List<RelationshipSummary> {
        val friends = friendRepository.findAllIncomingPending(me.id).toList()
        if (friends.isEmpty()) return emptyList()

        val otherIds = friends.map { if (it.userId == me.id) it.friendId else it.userId }
        val userMap = userService.findUserResponseByIds(otherIds, me.id).associateBy { it.id }

        return friends.mapNotNull { e ->
            val otherId = if (e.userId == me.id) e.friendId else e.userId
            userMap[otherId]?.let { other ->
                toRelationshipSummary(e, me.id, other, null)
            }
        }
    }


    /**
     * 친구 요청 보내기 (me -> targetUid) / 친구 성사되면 팔로우도 함께 등록
     */
    suspend fun request(me: UserEntity, targetUid: String) {
        val meId = me.id
        val targetId = requireUserIdByUid(targetUid)
        if (meId == targetId) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot friend yourself")

        val existing = friendRepository.findBetween(meId, targetId)
        when {
            existing == null -> {
                friendRepository.save(
                    FriendEntity(
                        userId = meId, friendId = targetId,
                        status = FriendStatus.PENDING.name,
                        statusAt = now, createdAt = now,
                    )
                )
//                expService.grantExp(
//                    userId = meId,
//                    actionCode = "FRIEND_ADD",
//                    targetId = targetId,
//                    requestId = meId,
//                )
                eventPublisher.publishEvent(
                    PushSendRequestedEvent(
                        userIds = listOf(targetId),
                        actorUserId = me.id,
                        templateData = PushTemplate.FRIEND_REQUEST.buildPushData("nickname" to me.nickname),
                    )
                )
                relationHistoryService.log(meId, targetId, RelationType.FRIEND.name, RelationAction.REQUEST.name)
            }
            existing.status == FriendStatus.PENDING.name -> {
                // 상대가 me 에게 이미 보낸 요청이라면, 이 요청은 '수락'으로 전환
                if (existing.userId == targetId && existing.friendId == meId) {
                    val accepted = existing.copy(
                        status = FriendStatus.ACCEPTED.name,
                        statusAt = now,
                        updatedAt = now
                    )
                    friendRepository.save(accepted)

                    // === 팔로우 등록 (양방향) ===
//                    followRepository.save(
//                        FollowEntity(
//                            userId = meId, followId = targetId,
//                            status = FollowStatus.ACCEPTED.name,
//                            statusAt = now, createdAt = now
//                        )
//                    )
//                    followRepository.save(
//                        FollowEntity(
//                            userId = targetId, followId = meId,
//                            status = FollowStatus.ACCEPTED.name,
//                            statusAt = now, createdAt = now
//                        )
//                    )
                    expService.grantExp(
                        userId = meId,
                        actionCode = "FRIEND_ADD",
                        targetId = targetId,
                        requestId = meId,
                    )
                    expService.grantExp(
                        userId = targetId,
                        actionCode = "FRIEND_ADD",
                        targetId = meId,
                        requestId = meId,
                    )
                    eventPublisher.publishEvent(
                        PushSendRequestedEvent(
                            userIds = listOf(targetId),
                            actorUserId = meId,
                            templateData = PushTemplate.FRIEND_ACCEPT.buildPushData("nickname" to me.nickname),
                        )
                    )
                    relationHistoryService.log(meId, targetId, RelationType.FRIEND.name, RelationAction.ACCEPT.name)
                } else {
                    // 내가 이미 보낸 상태면 중복요청
                    throw ResponseStatusException(HttpStatus.CONFLICT, "already requested")
                }
            }
            existing.status == FriendStatus.ACCEPTED.name ->
                throw ResponseStatusException(HttpStatus.CONFLICT, "already friends")
            existing.status == FriendStatus.BLOCKED.name ->
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "blocked")
            else ->
                throw ResponseStatusException(HttpStatus.CONFLICT, "cannot request in current state")
        }
    }

    /**
     * 받은 요청 수락 (requesterUid -> me)
     */
    suspend fun accept(me: UserEntity, requesterUid: String) {
        val meId = me.id
        val requesterId = requireUserIdByUid(requesterUid)
        val rel = friendRepository.findBetween(meId, requesterId)
            ?: throw IllegalStateException()

        if (rel.friendId != meId || rel.status != FriendStatus.PENDING.name) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "not a pending incoming request")
        }
        friendRepository.save(
            rel.copy(
                status = FriendStatus.ACCEPTED.name,
                statusAt = now,
                updatedAt = now
            )
        )


        // === 팔로우 등록 (양방향, 중복 체크) ===
//        if (!followRepository.existsByUserIdAndFollowId(meId, requesterId)) {
//            followRepository.save(
//                FollowEntity(
//                    userId = meId,
//                    followId = requesterId,
//                    status = FollowStatus.ACCEPTED.name,
//                    statusAt = now,
//                    createdAt = now
//                )
//            )
//        }
//
//        if (!followRepository.existsByUserIdAndFollowId(requesterId, meId)) {
//            followRepository.save(
//                FollowEntity(
//                    userId = requesterId,
//                    followId = meId,
//                    status = FollowStatus.ACCEPTED.name,
//                    statusAt = now,
//                    createdAt = now
//                )
//            )
//        }


        expService.grantExp(
            userId = requesterId,
            actionCode = "FRIEND_ADD",
            targetId = meId,
            requestId = meId,
        )
        expService.grantExp(
            userId = meId,
            actionCode = "FRIEND_ADD",
            targetId = requesterId,
            requestId = requesterId,
        )
        eventPublisher.publishEvent(
            PushSendRequestedEvent(
                userIds = listOf(requesterId),
                actorUserId = meId,
                templateData = PushTemplate.FRIEND_ACCEPT.buildPushData("nickname" to me.nickname),
            )
        )
        relationHistoryService.log(meId, requesterId, RelationType.FRIEND.name, RelationAction.ACCEPT.name)
    }

    /**
     * 받은 요청 거절 (requesterUid -> me)
     * 정책: 기록을 REJECTED 로 남기거나, 바로 삭제. 여기선 삭제.
     * TODO 거절이력?
     */
    suspend fun reject(me: UserEntity, requesterUid: String) {
        val meId = me.id
        val requesterId = requireUserIdByUid(requesterUid)
        val rel = friendRepository.findBetween(meId, requesterId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "no request")

        if (rel.friendId != meId || rel.status != FriendStatus.PENDING.name) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "not a pending incoming request")
        }
        friendRepository.delete(rel)
        relationHistoryService.log(meId, requesterId, RelationType.FRIEND.name, RelationAction.DENIED.name)
    }

    /**
     * 내가 보낸 요청 취소 (me -> targetUid)
     */
    suspend fun cancel(me: UserEntity, requesterUid: String) {
        val meId = me.id
        val requesterId = requireUserIdByUid(requesterUid)
        val rel = friendRepository.findBetween(meId, requesterId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "no request")

        if (rel.userId != meId || rel.status != FriendStatus.PENDING.name) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "not a pending outgoing request")
        }
        friendRepository.delete(rel)
        relationHistoryService.log(meId, requesterId, RelationType.FRIEND.name, RelationAction.CANCEL.name)
    }

    /**
     * 친구 끊기 (양쪽 누구든 가능)
     */
    suspend fun unfriend(me: UserEntity, otherUid: String) {
        val meId = me.id
        val otherId = requireUserIdByUid(otherUid)
        val rel = friendRepository.findBetween(meId, otherId)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "not friends")
        if (rel.status != FriendStatus.ACCEPTED.name) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "not accepted")
        }
        friendRepository.delete(rel)

        // === 팔로우 관계도 제거 (양방향) ===
//        followRepository.findBetween(meId, otherId)?.let { followRepository.delete(it) }
//        followRepository.findBetween(otherId, meId)?.let { followRepository.delete(it) }
        relationHistoryService.log(meId, otherId, RelationType.FRIEND.name, RelationAction.REMOVE.name)
    }

    /**
     * 검색: 결과에 현재 관계도 함께 태깅
     */
    suspend fun search(me: UserEntity, q: String, limit: Int = 30): List<RelationshipSearchItem> {
        val meId = me.id

        val publics = userRepository.searchSummary(q, limit)
            .filter { it.uid != me.uid.toString() }
            .toList()

        if (publics.isEmpty()) return emptyList()

        // 검색된 사용자들의 ID 목록 추출
        val otherIds = publics.map { it.id }

        // 친구 관계 한 번에 조회 (N+1 최적화)
        val relations = friendRepository.findAllBetweenBulk(meId, otherIds).toList()
        val relationMap = relations.associateBy { if (it.userId == meId) it.friendId else it.userId }

        return publics.map { pu ->
            val rel = relationMap[pu.id]
            val relation = when {
                rel == null -> null
                rel.status == FriendStatus.ACCEPTED.name -> "ACCEPTED"
                rel.status == FriendStatus.PENDING.name && rel.userId == meId -> "PENDING_OUT"
                rel.status == FriendStatus.PENDING.name && rel.friendId == meId -> "PENDING_IN"
                rel.status == FriendStatus.BLOCKED.name -> "BLOCKED"
                else -> rel.status
            }
            RelationshipSearchItem(
                uid = pu.uid,
                username = pu.username,
                nickname = pu.nickname,
                relation = relation
            )
        }
    }


    suspend fun updateLocationMode(userId: Long, friendId: Long, mode: LocationMode) {
        val friendship = friendRepository.findBetween(userId, friendId)
            ?: throw IllegalArgumentException("친구 관계가 존재하지 않습니다.")

        val updated = if (friendship.userId == userId) {
            friendship.copy(userLocationMode = mode, updatedAt = OffsetDateTime.now())
        } else {
            friendship.copy(friendLocationMode = mode, updatedAt = OffsetDateTime.now())
        }

        friendRepository.save(updated)
    }


}
