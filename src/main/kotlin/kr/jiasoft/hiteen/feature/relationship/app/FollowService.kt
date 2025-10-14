package kr.jiasoft.hiteen.feature.relationship.app

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.level.app.ExpService
import kr.jiasoft.hiteen.feature.push.app.PushService
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
import kr.jiasoft.hiteen.feature.push.domain.buildPushData
import kr.jiasoft.hiteen.feature.relationship.domain.FollowEntity
import kr.jiasoft.hiteen.feature.relationship.domain.FollowStatus
import kr.jiasoft.hiteen.feature.relationship.dto.RelationshipSummary
import kr.jiasoft.hiteen.feature.relationship.infra.FollowRepository
import kr.jiasoft.hiteen.feature.user.app.UserService
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.dto.UserResponse
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class FollowService(
    private val followRepository: FollowRepository,
    private val userRepository: UserRepository,
    private val userService: UserService,
    private val expService: ExpService,
    private val pushService: PushService,
) {
    private val now: OffsetDateTime get() = OffsetDateTime.now(ZoneOffset.UTC)

    private suspend fun resolveIds(me: UserEntity, otherUid: String): Pair<Long, Long> {
        val meId = me.id
        val otherId = userRepository.findByUid(otherUid)?.id
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "user not found: $otherUid")
        if (meId == otherId) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot follow yourself")
        return meId to otherId
    }

    private suspend fun getRelationOrThrow(meId: Long, otherId: Long, direction: String): FollowEntity {
        return followRepository.findBetween(meId, otherId)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "no follow request in $direction direction")
    }

    private fun toFollowSummary(e: FollowEntity, other: UserResponse?): RelationshipSummary =
        RelationshipSummary(
            userResponse = other ?: UserResponse.empty(),
            status = e.status,
            statusAt = e.statusAt
        )


    /** 내가 팔로우하고 있는 목록 (Following) */
    suspend fun listFollowing(me: UserEntity): List<RelationshipSummary> {
        return followRepository.findAllByUserIdAndStatus(me.id, FollowStatus.ACCEPTED.name)
            .map { e ->
                val other = userService.findUserResponse(e.followId, me.id)
                toFollowSummary(e, other)
            }.toList()
    }

    /** 나를 팔로우하는 목록 (Followers) */
    suspend fun listFollowers(me: UserEntity): List<RelationshipSummary> {
        return followRepository.findAllByFollowIdAndStatus(me.id, FollowStatus.ACCEPTED.name)
            .map { e ->
                val other = userService.findUserResponse(e.followId, me.id)
                toFollowSummary(e, other)
            }.toList()
    }

    /** 내가 보낸 팔로우 요청 (아직 수락 안 됨) */
    suspend fun listOutgoing(me: UserEntity): List<RelationshipSummary> {
        return followRepository.findAllByUserIdAndStatus(me.id, FollowStatus.PENDING.name)
            .map { e ->
                val other = userService.findUserResponse(e.followId, me.id)
                toFollowSummary(e, other)
            }.toList()
    }

    /** 내가 받은 팔로우 요청 (아직 수락 안 됨) */
    suspend fun listIncoming(me: UserEntity): List<RelationshipSummary> {
        return followRepository.findAllByFollowIdAndStatus(me.id, FollowStatus.PENDING.name)
            .map { e ->
                val other = userService.findUserResponse(e.followId, me.id)
                toFollowSummary(e, other)
            }.toList()
    }


    /** 팔로우 요청 보내기 */
    suspend fun request(me: UserEntity, otherUid: String) {
        val (meId, otherId) = resolveIds(me, otherUid)

        val existing = followRepository.findBetween(meId, otherId)
        when {
            existing == null -> {
                followRepository.save(
                    FollowEntity(
                        userId = meId,
                        followId = otherId,
                        status = FollowStatus.PENDING.name,
                        statusAt = now,
                        createdAt = now
                    )
                )
                expService.grantExp(meId, "FOLLOW_REQUEST", otherId)
                pushService.sendAndSavePush(
                    listOf(otherId),
                    PushTemplate.FOLLOW_REQUEST.buildPushData("nickname" to me.nickname)
                )
            }
            existing.status == FollowStatus.PENDING.name ->
                throw ResponseStatusException(HttpStatus.CONFLICT, "already requested")
            existing.status == FollowStatus.ACCEPTED.name ->
                throw ResponseStatusException(HttpStatus.CONFLICT, "already following")
            else ->
                throw ResponseStatusException(HttpStatus.CONFLICT, "cannot request in current state")
        }
    }

    /** 받은 팔로우 요청 승인 */
    suspend fun accept(me: UserEntity, otherUid: String) {
        val (meId, otherId) = resolveIds(me, otherUid)
        val rel = getRelationOrThrow(otherId, meId, "incoming")

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
        expService.grantExp(meId, "FOLLOW_ACCEPT", otherId)
        pushService.sendAndSavePush(
            listOf(otherId),
            PushTemplate.FOLLOW_ACCEPT.buildPushData("nickname" to me.nickname)
        )
    }

    /** 받은 팔로우 요청 거절 (삭제) */
    suspend fun reject(me: UserEntity, otherUid: String) {
        val (meId, otherId) = resolveIds(me, otherUid)
        val rel = getRelationOrThrow(otherId, meId, "incoming")

        if (rel.followId != meId || rel.status != FollowStatus.PENDING.name) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "not a pending incoming request")
        }

        followRepository.delete(rel)
    }

    /** 내가 보낸 팔로우 요청 취소 */
    suspend fun cancel(me: UserEntity, otherUid: String) {
        val (meId, otherId) = resolveIds(me, otherUid)
        val rel = getRelationOrThrow(meId, otherId, "outgoing")

        if (rel.userId != meId || rel.status != FollowStatus.PENDING.name) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "not a pending outgoing request")
        }

        followRepository.delete(rel)
    }

    /** 언팔로우 (이미 승인된 상태 끊기) */
    suspend fun unfollow(me: UserEntity, otherUid: String) {
        val (meId, otherId) = resolveIds(me, otherUid)
        val rel = getRelationOrThrow(meId, otherId, "outgoing")

        if (rel.userId != meId || rel.status != FollowStatus.ACCEPTED.name) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "not an active follow")
        }

        followRepository.delete(rel)
    }

    /** 나를 팔로우하는 사람 강제 제거 (내 follower 끊기) */
    suspend fun removeFollower(me: UserEntity, otherUid: String) {
        val meId = me.id
        val otherId = userRepository.findByUid(otherUid)?.id
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "user not found: $otherUid")

        val rel = followRepository.findBetween(otherId, meId)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "not a follower")

        if (rel.followId != meId || rel.status != FollowStatus.ACCEPTED.name) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "not an active follower")
        }

        followRepository.delete(rel)
    }



}
