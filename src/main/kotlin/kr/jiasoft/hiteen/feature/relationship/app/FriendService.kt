package kr.jiasoft.hiteen.feature.relationship.app

import kr.jiasoft.hiteen.feature.relationship.domain.FriendEntity
import kr.jiasoft.hiteen.feature.relationship.domain.FriendStatus
import kr.jiasoft.hiteen.feature.relationship.dto.FriendListResponse
import kr.jiasoft.hiteen.feature.relationship.dto.FriendSearchItem
import kr.jiasoft.hiteen.feature.relationship.dto.FriendSearchResponse
import kr.jiasoft.hiteen.feature.relationship.dto.FriendSummary
import kr.jiasoft.hiteen.feature.relationship.infra.FriendRepository
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.dto.PublicUser
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class FriendService(
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository
) {
    private val now: OffsetDateTime get() = OffsetDateTime.now(ZoneOffset.UTC)

    private suspend fun requireUserIdByUid(uid: String): Long {
        return userRepository.findByUid(uid)?.id
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "user not found: $uid")
    }

    private fun toSummary(meId: Long, e: FriendEntity, other: PublicUser?): FriendSummary {
        val otherUid = other?.uid ?: ""
        return FriendSummary(
            uid = otherUid,
            username = other?.username ?: "",
            nickname = other?.nickname,
            telno = other?.telno,
            address = other?.address,
            detailAddress = other?.detailAddress,
            mood = other?.mood,
            tier = other?.tier,
            assetUid = other?.assetUid,
            status = e.status,
            statusAt = e.statusAt
        )
    }

    suspend fun listFriends(me: UserEntity): FriendListResponse {
        val rows = friendRepository.findAllAccepted(me.id!!)
        val items = rows.map { e ->
            val otherId = if (e.userId == me.id) e.friendId else e.userId
            val other = userRepository.findPublicById(otherId)
            toSummary(me.id!!, e, other)
        }
        return FriendListResponse(items)
    }

    suspend fun listOutgoing(me: UserEntity): FriendListResponse {
        val rows = friendRepository.findAllOutgoingPending(me.id!!)
        val items = rows.map { e ->
            val other = userRepository.findPublicById(e.friendId)
            toSummary(me.id!!, e, other)
        }
        return FriendListResponse(items)
    }

    suspend fun listIncoming(me: UserEntity): FriendListResponse {
        val rows = friendRepository.findAllIncomingPending(me.id!!)
        val items = rows.map { e ->
            val other = userRepository.findPublicById(e.userId)
            toSummary(me.id!!, e, other)
        }
        return FriendListResponse(items)
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
                        updatedAt = now
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
    suspend fun cancel(me: UserEntity, targetUid: String) {
        val meId = me.id!!
        val targetId = requireUserIdByUid(targetUid)
        val rel = friendRepository.findBetween(meId, targetId)
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
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "not friends")
        if (rel.status != FriendStatus.ACCEPTED.name) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "not accepted")
        }
        friendRepository.delete(rel)
    }

    /**
     * 검색: 결과에 현재 관계도 함께 태깅
     */
    suspend fun search(me: UserEntity, q: String, limit: Int = 30): FriendSearchResponse {
        val meId = me.id!!
        val publics = userRepository.searchPublic(q, limit).filter { it.uid != me.uid.toString() }
        val results = publics.map { pu ->
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
            FriendSearchItem(
                uid = pu.uid, username = pu.username, nickname = pu.nickname, relation = relation
            )
        }
        return FriendSearchResponse(results)
    }
}
