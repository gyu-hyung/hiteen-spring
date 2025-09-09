package kr.jiasoft.hiteen.feature.relationship.app

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.location.domain.LocationHistory
import kr.jiasoft.hiteen.feature.location.infra.cache.LocationCacheRedisService
import kr.jiasoft.hiteen.feature.relationship.domain.FriendEntity
import kr.jiasoft.hiteen.feature.relationship.domain.FriendStatus
import kr.jiasoft.hiteen.feature.relationship.dto.ContactResponse
import kr.jiasoft.hiteen.feature.relationship.dto.RelationshipSummary
import kr.jiasoft.hiteen.feature.relationship.dto.RelationshipSearchItem
import kr.jiasoft.hiteen.feature.relationship.infra.FriendRepository
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.dto.UserSummary
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class FriendService(
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository,
    private val locationCacheRedisService: LocationCacheRedisService
) {
    private val now: OffsetDateTime get() = OffsetDateTime.now(ZoneOffset.UTC)

    private suspend fun requireUserIdByUid(uid: String): Long {
        return userRepository.findByUid(uid)?.id
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "user not found: $uid")
    }

    private fun toRelationshipSummary(e: FriendEntity, other: UserSummary?, latestLocation: LocationHistory?): RelationshipSummary {
        return RelationshipSummary(
            userSummary = other ?: UserSummary.empty(),
            status = e.status,
            statusAt = e.statusAt,
            lat = latestLocation?.lat,
            lng = latestLocation?.lng,
            lastSeenAt = latestLocation?.timestamp
        )
    }


    suspend fun getContacts(user: UserEntity, rawContacts: String): ContactResponse {
        // 1. 연락처 문자열 -> 전화번호 리스트
        val phones = rawContacts.split("\n")
            .map { it.filter { ch -> ch.isDigit() } }
            .filter { it.isNotBlank() }
            .toSet()

        // 2. 가입 사용자 조회 (연락처에 해당하는 users)
        val registeredUsers = userRepository.findAllByPhoneIn(phones).toList()

        // 3. 친구 관계 조회 (내가 user_id 또는 friend_id 인 경우)
        val friends = friendRepository.findByUserIdOrFriendId(user.id!!, user.id).toList()

        // 3-1. 친구 userId 집합 만들기 (내 친구들의 userId)
        val friendIds = friends.map {
            if (it.userId == user.id) it.friendId else it.userId
        }.toSet()

        // 4. 그룹 분류
//        val friendList = registeredUsers.filter { it.id in friendIds }
        val registeredNotFriend = registeredUsers.filter { it.id !in friendIds && it.id != user.id }
        val notRegistered = phones.filter { phone -> registeredUsers.none { it.phone == phone } }

        return ContactResponse(
            registeredUsers = registeredNotFriend.map { UserSummary.from(it) },
//            friends = friendList.map { ContactDto.from(it, "friend") },
            notRegisteredUsers = notRegistered
        )
    }

    suspend fun listFriends(me: UserEntity): List<RelationshipSummary> {
        return friendRepository.findAllAccepted(me.id!!)
            .map { e ->
                val otherId = if (e.userId == me.id) e.friendId else e.userId
                val other = userRepository.findSummaryInfoById(otherId)

                // Redis/Mongo에서 최신 위치 조회 (uid기준)
                val latestLocation = other?.uid?.let { uid ->
                    locationCacheRedisService.getLatest(uid)
                }

                toRelationshipSummary(e, other, latestLocation)
            }.toList()
    }


    suspend fun listOutgoing(me: UserEntity): List<RelationshipSummary> {
        return friendRepository.findAllOutgoingPending(me.id!!)
            .map { e ->
                val other = userRepository.findSummaryInfoById(e.friendId)
                toRelationshipSummary(e, other, null)
            }.toList()
    }


    suspend fun listIncoming(me: UserEntity): List<RelationshipSummary> {
        return friendRepository.findAllIncomingPending(me.id!!)
            .map { e ->
                val other = userRepository.findSummaryInfoById(e.userId)
                toRelationshipSummary(e, other, null)
            }.toList()
    }


    /**
     * 친구 요청 보내기 (me -> targetUid)
     */
    suspend fun request(me: UserEntity, targetUid: String) {
        val meId = me.id!!
        val targetId = requireUserIdByUid(targetUid)
        if (meId == targetId) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot friend yourself")

        val existing = friendRepository.findBetween(meId, targetId)
        when {
            existing == null -> {
                friendRepository.save(
                    FriendEntity(
                        userId = meId,
                        friendId = targetId,
                        status = FriendStatus.PENDING.name,
                        statusAt = now,
                        createdAt = now,
                    )
                )
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
        val meId = me.id!!
        val requesterId = requireUserIdByUid(requesterUid)
        val rel = friendRepository.findBetween(meId, requesterId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "no request")

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
    }

    /**
     * 받은 요청 거절 (requesterUid -> me)
     * 정책: 기록을 REJECTED 로 남기거나, 바로 삭제. 여기선 삭제.
     * TODO 거절이력?
     */
    suspend fun reject(me: UserEntity, requesterUid: String) {
        val meId = me.id!!
        val requesterId = requireUserIdByUid(requesterUid)
        val rel = friendRepository.findBetween(meId, requesterId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "no request")

        if (rel.friendId != meId || rel.status != FriendStatus.PENDING.name) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "not a pending incoming request")
        }
        friendRepository.delete(rel)
    }

    /**
     * 내가 보낸 요청 취소 (me -> targetUid)
     */
    suspend fun cancel(me: UserEntity, requesterUid: String) {
        val meId = me.id!!
        val requesterId = requireUserIdByUid(requesterUid)
        val rel = friendRepository.findBetween(meId, requesterId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "no request")

        if (rel.userId != meId || rel.status != FriendStatus.PENDING.name) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "not a pending outgoing request")
        }
        friendRepository.delete(rel)
    }

    /**
     * 친구 끊기 (양쪽 누구든 가능)
     */
    suspend fun unfriend(me: UserEntity, otherUid: String) {
        val meId = me.id!!
        val otherId = requireUserIdByUid(otherUid)
        val rel = friendRepository.findBetween(meId, otherId)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "not friends")
        if (rel.status != FriendStatus.ACCEPTED.name) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "not accepted")
        }
        friendRepository.delete(rel)
    }

    /**
     * 검색: 결과에 현재 관계도 함께 태깅
     */
    suspend fun search(me: UserEntity, q: String, limit: Int = 30): List<RelationshipSearchItem> {
        val meId = me.id!!

        val publics = userRepository.searchPublic(q, limit)
            .filter { it.uid != me.uid.toString() }
            .toList()

        return publics.map { pu ->
            val otherId = requireUserIdByUid(pu.uid)
            val rel = friendRepository.findBetween(meId, otherId)
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

}
