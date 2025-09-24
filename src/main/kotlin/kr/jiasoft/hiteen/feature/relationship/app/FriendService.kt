package kr.jiasoft.hiteen.feature.relationship.app

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.level.app.ExpService
import kr.jiasoft.hiteen.feature.location.domain.LocationHistory
import kr.jiasoft.hiteen.feature.location.infra.cache.LocationCacheRedisService
import kr.jiasoft.hiteen.feature.relationship.domain.FollowEntity
import kr.jiasoft.hiteen.feature.relationship.domain.FollowStatus
import kr.jiasoft.hiteen.feature.relationship.domain.FriendEntity
import kr.jiasoft.hiteen.feature.relationship.domain.FriendStatus
import kr.jiasoft.hiteen.feature.relationship.domain.LocationMode
import kr.jiasoft.hiteen.feature.relationship.dto.ContactResponse
import kr.jiasoft.hiteen.feature.relationship.dto.RelationshipSummary
import kr.jiasoft.hiteen.feature.relationship.dto.RelationshipSearchItem
import kr.jiasoft.hiteen.feature.relationship.infra.FollowRepository
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
    private val followRepository: FollowRepository,
    private val userRepository: UserRepository,
    private val locationCacheRedisService: LocationCacheRedisService,
    private val expService: ExpService
) {
    private val now: OffsetDateTime get() = OffsetDateTime.now(ZoneOffset.UTC)

    private suspend fun requireUserIdByUid(uid: String): Long {
        return userRepository.findByUid(uid)?.id
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "user not found: $uid")
    }

    private fun toRelationshipSummary(
            e: FriendEntity,
            myId: Long,
            other: UserSummary?,
            latestLocation: LocationHistory?
    ): RelationshipSummary {
        val myMode = if (e.userId == myId) e.userLocationMode else e.friendLocationMode
        val theirMode = if (e.userId == myId) e.friendLocationMode else e.userLocationMode

        return RelationshipSummary(
            userSummary = other ?: UserSummary.empty(),
            status = e.status,
            statusAt = e.statusAt,
            lat = latestLocation?.lat,
            lng = latestLocation?.lng,
            lastSeenAt = latestLocation?.timestamp,
            myLocationMode = myMode,
            theirLocationMode = theirMode
        )
    }

    suspend fun isFriend(userId: Long, targetId: Long): Boolean {
        return friendRepository.existsFriend(userId, targetId) > 0
    }

    suspend fun findUserByUid(uid: String): UserEntity? {
        return userRepository.findByUid(uid)
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
        val friendRelations = friendRepository.findByUserIdOrFriendId(user.id, user.id).toList()

        // 3-1. 친구 userId 집합 만들기 (내 친구들의 userId)
        val friendIds = friendRelations.map {
            if (it.userId == user.id) it.friendId else it.userId
        }.toSet()

        // 4. 그룹 분류
        val friendList = registeredUsers.filter { it.id in friendIds }
        val registeredNotFriend = registeredUsers.filter { it.id !in friendIds && it.id != user.id }
        val notRegistered = phones.filter { phone -> registeredUsers.none { it.phone == phone } }

        return ContactResponse(
            registeredUsers = registeredNotFriend.map { u ->
                UserSummary.from(u, isFriend = u.id in friendIds)
            },
            friends = friendList.map { u ->
                UserSummary.from(u, isFriend = true)
            },
            notRegisteredUsers = notRegistered
        )
    }

    suspend fun listFriends(me: UserEntity): List<RelationshipSummary> {
        return friendRepository.findAllAccepted(me.id)
            .map { e ->
                val otherId = if (e.userId == me.id) e.friendId else e.userId
                val other = userRepository.findSummaryInfoById(otherId)

                // Redis/Mongo에서 최신 위치 조회 (uid기준)
                val latestLocation = other.uid.let { uid ->
                    locationCacheRedisService.getLatest(uid)
                }

                toRelationshipSummary(e, me.id, other, latestLocation)
            }.toList()
    }


    suspend fun listOutgoing(me: UserEntity): List<RelationshipSummary> {
        return friendRepository.findAllOutgoingPending(me.id)
            .map { e ->
                val other = userRepository.findSummaryInfoById(e.friendId)
                toRelationshipSummary(e, me.id, other, null)
            }.toList()
    }


    suspend fun listIncoming(me: UserEntity): List<RelationshipSummary> {
        return friendRepository.findAllIncomingPending(me.id)
            .map { e ->
                val other = userRepository.findSummaryInfoById(e.userId)
                toRelationshipSummary(e, me.id, other, null)
            }.toList()
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
                    followRepository.save(
                        FollowEntity(
                            userId = meId, followId = targetId,
                            status = FollowStatus.ACCEPTED.name,
                            statusAt = now, createdAt = now
                        )
                    )
                    followRepository.save(
                        FollowEntity(
                            userId = targetId, followId = meId,
                            status = FollowStatus.ACCEPTED.name,
                            statusAt = now, createdAt = now
                        )
                    )
                    expService.grantExp(meId, "FRIEND_ADD", targetId)
                    expService.grantExp(targetId, "FRIEND_ADD", meId)
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

        // === 팔로우 등록 (양방향) ===
        followRepository.save(
            FollowEntity(userId = meId, followId = requesterId,
                status = FollowStatus.ACCEPTED.name,
                statusAt = now, createdAt = now)
        )
        followRepository.save(
            FollowEntity(userId = requesterId, followId = meId,
                status = FollowStatus.ACCEPTED.name,
                statusAt = now, createdAt = now)
        )

        expService.grantExp(meId, "FRIEND_ADD", requesterId)
        expService.grantExp(requesterId, "FRIEND_ADD", meId)
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
        followRepository.findBetween(meId, otherId)?.let { followRepository.delete(it) }
        followRepository.findBetween(otherId, meId)?.let { followRepository.delete(it) }
    }

    /**
     * 검색: 결과에 현재 관계도 함께 태깅
     */
    suspend fun search(me: UserEntity, q: String, limit: Int = 30): List<RelationshipSearchItem> {
        val meId = me.id

        val publics = userRepository.searchSummary(q, limit)
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
