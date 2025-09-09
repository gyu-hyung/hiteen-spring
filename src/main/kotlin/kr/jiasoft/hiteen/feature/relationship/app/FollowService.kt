package kr.jiasoft.hiteen.feature.relationship.app

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.relationship.domain.FollowEntity
import kr.jiasoft.hiteen.feature.relationship.domain.FollowStatus
import kr.jiasoft.hiteen.feature.relationship.dto.RelationshipSearchItem
import kr.jiasoft.hiteen.feature.relationship.dto.RelationshipSummary
import kr.jiasoft.hiteen.feature.relationship.infra.FollowRepository
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.dto.UserSummary
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.time.ZoneOffset


@Service
class FollowService(
    private val followRepository: FollowRepository,
    private val userRepository: UserRepository
) {
    private val now: OffsetDateTime get() = OffsetDateTime.now(ZoneOffset.UTC)

    private suspend fun requireUserIdByUid(uid: String): Long {
        return userRepository.findByUid(uid)?.id
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "user not found: $uid")
    }

    private fun toFollowSummary(e: FollowEntity, other: UserSummary?): RelationshipSummary {
        return RelationshipSummary(
            userSummary = other ?: UserSummary.empty(),
            status = e.status,
            statusAt = e.statusAt
        )
    }

    suspend fun listFriends(me: UserEntity): List<RelationshipSummary> {
        return followRepository.findAllAccepted(me.id!!)
            .map { e ->
                val otherId = if (e.userId == me.id) e.followId else e.userId
                val other = userRepository.findSummaryInfoById(otherId)
                toFollowSummary(e, other)
            }.toList()
    }


    suspend fun listOutgoing(me: UserEntity): List<RelationshipSummary> {
        return followRepository.findAllOutgoingPending(me.id!!)
            .map { e ->
                val other = userRepository.findSummaryInfoById(e.followId)
                toFollowSummary(e, other)
            }.toList()
    }


    suspend fun listIncoming(me: UserEntity): List<RelationshipSummary> {
        return followRepository.findAllIncomingPending(me.id!!)
            .map { e ->
                val other = userRepository.findSummaryInfoById(e.userId)
                toFollowSummary(e, other)
            }.toList()
    }

    /**
     * 친구 요청 보내기 (me -> targetUid)
     */
    suspend fun request(me: UserEntity, targetUid: String) {
        val meId = me.id!!
        val targetId = requireUserIdByUid(targetUid)
        if (meId == targetId) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot friend yourself")

        val existing = followRepository.findBetween(meId, targetId)
        when {
            existing == null -> {
                followRepository.save(
                    FollowEntity(
                        userId = meId,
                        followId = targetId,
                        status = FollowStatus.PENDING.name,
                        statusAt = now,
                        createdAt = now,
                    )
                )
            }
            existing.status == FollowStatus.PENDING.name -> {
                // 상대가 me 에게 이미 보낸 요청이라면, 이 요청은 '수락'으로 전환
                if (existing.userId == targetId && existing.followId == meId) {
                    val accepted = existing.copy(
                        status = FollowStatus.ACCEPTED.name,
                        statusAt = now,
                        updatedAt = now
                    )
                    followRepository.save(accepted)
                } else {
                    // 내가 이미 보낸 상태면 중복요청
                    throw ResponseStatusException(HttpStatus.CONFLICT, "already requested")
                }
            }
            existing.status == FollowStatus.ACCEPTED.name ->
                throw ResponseStatusException(HttpStatus.CONFLICT, "already friends")
            existing.status == FollowStatus.BLOCKED.name ->
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
        val rel = followRepository.findBetween(meId, requesterId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "no request")

        if (rel.followId != meId || rel.status != FollowStatus.PENDING.name) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "not a pending incoming request")
        }
        followRepository.save(
            rel.copy(
                status = FollowStatus.ACCEPTED.name,
                statusAt = now,
                updatedAt = now
            )
        )
    }

    /**
     * 받은 요청 거절 (requesterUid -> me)
     * 정책: 기록을 REJECTED 로 남기거나, 바로 삭제. 여기선 삭제.
     */
    suspend fun reject(me: UserEntity, requesterUid: String) {
        val meId = me.id!!
        val requesterId = requireUserIdByUid(requesterUid)
        val rel = followRepository.findBetween(meId, requesterId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "no request")

        if (rel.followId != meId || rel.status != FollowStatus.PENDING.name) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "not a pending incoming request")
        }
        followRepository.delete(rel)
    }

    /**
     * 내가 보낸 요청 취소 (me -> targetUid)
     */
    suspend fun cancel(me: UserEntity, targetUid: String) {
        val meId = me.id!!
        val targetId = requireUserIdByUid(targetUid)
        val rel = followRepository.findBetween(meId, targetId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "no request")
        if (rel.userId != meId || rel.status != FollowStatus.PENDING.name) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "not a pending outgoing request")
        }
        followRepository.delete(rel)
    }

    /**
     * 친구 끊기 (양쪽 누구든 가능)
     */
    suspend fun unfriend(me: UserEntity, otherUid: String) {
        val meId = me.id!!
        val otherId = requireUserIdByUid(otherUid)
        val rel = followRepository.findBetween(meId, otherId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "not friends")
        if (rel.status != FollowStatus.ACCEPTED.name) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "not accepted")
        }
        followRepository.delete(rel)
    }

    /**
     * 검색: 결과에 현재 관계도 함께 태깅
     */
    suspend fun search(me: UserEntity, q: String, limit: Int = 30): List<RelationshipSearchItem> {
        val meId = me.id!!
        val publics = userRepository.searchPublic(q, limit)
            .filter { it.uid != me.uid.toString() }
            .toList()

        val results = publics.map { pu ->
            val otherId = requireUserIdByUid(pu.uid)
            val rel = followRepository.findBetween(meId, otherId)
            val relation = when {
                rel == null -> null
                rel.status == FollowStatus.ACCEPTED.name -> "ACCEPTED"
                rel.status == FollowStatus.PENDING.name && rel.userId == meId -> "PENDING_OUT"
                rel.status == FollowStatus.PENDING.name && rel.followId == meId -> "PENDING_IN"
                rel.status == FollowStatus.BLOCKED.name -> "BLOCKED"
                else -> rel.status
            }
            RelationshipSearchItem(
                uid = pu.uid,
                username = pu.username,
                nickname = pu.nickname,
                relation = relation
            )
        }

        return results
    }
}
