package kr.jiasoft.hiteen.feature.block.app

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.block.domain.UserBlockEntity
import kr.jiasoft.hiteen.feature.block.dto.BlockedUserResponse
import kr.jiasoft.hiteen.feature.block.infra.UserBlockRepository
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime

@Service
class UserBlockService(
    private val userBlockRepository: UserBlockRepository,
    private val userRepository: UserRepository,
) {

    /**
     * 사용자 차단
     */
    suspend fun blockUser(userId: Long, targetUid: String, reason: String?): UserBlockEntity {
        val targetUser = userRepository.findByUid(targetUid)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.")

        if (userId == targetUser.id) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 자신을 차단할 수 없습니다.")
        }

        // 이미 차단되어 있는지 확인
        if (userBlockRepository.existsByUserIdAndBlockedUserId(userId, targetUser.id)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "이미 차단한 사용자입니다.")
        }

        return userBlockRepository.save(
            UserBlockEntity(
                userId = userId,
                blockedUserId = targetUser.id,
                reason = reason,
                createdAt = OffsetDateTime.now(),
            )
        )
    }

    /**
     * 사용자 차단 해제
     */
    suspend fun unblockUser(userId: Long, targetUid: String) {
        val targetUser = userRepository.findByUid(targetUid)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.")

        val deleted = userBlockRepository.deleteByUserIdAndBlockedUserId(userId, targetUser.id)
        if (deleted == 0L) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "차단 관계가 존재하지 않습니다.")
        }
    }

    /**
     * 차단한 사용자 목록 조회
     */
    suspend fun getBlockedUsers(userId: Long): List<BlockedUserResponse> {
        val blocks = userBlockRepository.findAllByUserId(userId).toList()
        if (blocks.isEmpty()) return emptyList()

        val blockedUserIds = blocks.map { it.blockedUserId }
        val userMap = userRepository.findAllById(blockedUserIds).toList().associateBy { it.id }

        return blocks.mapNotNull { block ->
            val user = userMap[block.blockedUserId] ?: return@mapNotNull null
            BlockedUserResponse(
                id = block.id,
                blockedUserUid = user.uid.toString(),
                blockedUserNickname = user.nickname,
                blockedUserAssetUid = user.assetUid?.toString(),
                reason = block.reason,
                createdAt = block.createdAt,
            )
        }
    }

    /**
     * 차단 여부 확인
     */
    suspend fun isBlocked(userId: Long, targetUserId: Long): Boolean {
        return userBlockRepository.existsByUserIdAndBlockedUserId(userId, targetUserId)
    }

    /**
     * 차단한 사용자 ID 목록 조회 (콘텐츠 필터링용)
     */
    suspend fun getBlockedUserIds(userId: Long): Set<Long> {
        return userBlockRepository.findBlockedUserIdsByUserId(userId).toList().toSet()
    }

    /**
     * 차단한 사용자 수 조회
     */
    suspend fun getBlockedUserCount(userId: Long): Long {
        return userBlockRepository.countByUserId(userId)
    }
}

