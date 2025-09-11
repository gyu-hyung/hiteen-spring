package kr.jiasoft.hiteen.feature.interest.app

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.common.exception.BusinessValidationException
import kr.jiasoft.hiteen.feature.interest.domain.InterestMatchHistoryEntity
import kr.jiasoft.hiteen.feature.interest.domain.InterestUserEntity
import kr.jiasoft.hiteen.feature.interest.dto.FriendRecommendationResponse
import kr.jiasoft.hiteen.feature.interest.dto.InterestUserResponse
import kr.jiasoft.hiteen.feature.interest.infra.InterestMatchHistoryRepository
import kr.jiasoft.hiteen.feature.interest.infra.InterestUserRepository
import kr.jiasoft.hiteen.feature.school.infra.SchoolRepository
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.dto.UserResponse
import kr.jiasoft.hiteen.feature.user.infra.UserPhotosRepository
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class InterestUserService(
    private val interestUserRepository: InterestUserRepository,
    private val interestMatchHistoryRepository: InterestMatchHistoryRepository,
    private val userPhotosRepository: UserPhotosRepository,
    private val userRepository: UserRepository,
    private val schoolRepository: SchoolRepository
) {

    /** 특정 사용자 관심사 등록 */
    suspend fun addInterestToUser(user: UserEntity, interestId: Long): InterestUserResponse? {
        val exist = interestUserRepository.findByUserIdAndInterestId(user.id!!, interestId)
        if (exist != null) {
            return interestUserRepository.getInterestResponseById(exist.id!!, null).firstOrNull()
        }

        val entity = InterestUserEntity(
            interestId = interestId,
            userId = user.id,
            createdAt = OffsetDateTime.now()
        )
        val saved = interestUserRepository.save(entity)
        return interestUserRepository.getInterestResponseById(saved.id!!, null).firstOrNull()
    }

    /** 특정 사용자의 모든 관심사 조회 */
    suspend fun getUserInterests(userUid: String): List<InterestUserResponse> {
        val userEntity = userRepository.findByUid(userUid)
        return interestUserRepository.getInterestResponseById(null, userEntity?.id!!).toList()
    }

    /** 특정 관심사 삭제 */
    suspend fun removeInterestFromUser(userId: Long, interestId: Long): Boolean {
        val deletedCount = interestUserRepository.deleteByUserIdAndInterestId(userId, interestId)
        return deletedCount > 0
    }

    /** 특정 사용자의 모든 관심사 초기화 */
    suspend fun clearUserInterests(userId: Long) {
        interestUserRepository.findByUserId(userId).collect { interestUserRepository.delete(it) }
    }


    /** 오늘의 추천 친구 1명 뽑기 */
    suspend fun recommendFriend(user: UserEntity, dailyLimit: Int = 1): FriendRecommendationResponse? {
        val todayCount = interestMatchHistoryRepository.countTodayRecommendations(user.id!!)
        if (todayCount >= dailyLimit) {
            return null
        }

        // 1) 내 관심사 목록
        val myInterests = interestUserRepository.findByUserId(user.id).toList().map { it.interestId }.toSet()
        if (myInterests.isEmpty()) {
            throw BusinessValidationException(mapOf("message" to "관심사가 없습니다."))
        }

        // 2) 후보군 ID
        val candidateUserIds = interestUserRepository.findUsersByInterestIds(myInterests, user.id).toList()
        val excludedUserIds = interestMatchHistoryRepository.findTargetIdsByUserId(user.id).toList()
        val availableUserIds = candidateUserIds.filterNot { excludedUserIds.contains(it) }

        val targetUserId = availableUserIds.shuffled().firstOrNull() ?: return null

        // 3) 실제 UserEntity → UserResponse 변환
        val targetUser = userRepository.findById(targetUserId)
            ?: return null
        val school = targetUser.schoolId?.let { schoolRepository.findById(it) }
        val targetUserResponse = UserResponse.from(targetUser, school)

        // 4) 추천 대상자의 관심사 목록
        val interests = interestUserRepository.getInterestResponseById(null, targetUserId).toList()

        // 5) 추천 대상자의 사진 목록
        val photos = userPhotosRepository.findByUserId(targetUserId).toList()

        // 추천 이력 저장
        interestMatchHistoryRepository.save(
            InterestMatchHistoryEntity(
                id = null,
                userId = user.id,
                targetId = targetUser.id!!,
                status = "RECOMMENDED",
                createdAt = OffsetDateTime.now(),
            )
        )

        return FriendRecommendationResponse(
            user = targetUserResponse,
            interests = interests,
            photos = photos
        )
    }




    /** 오늘 패스하기 */
    suspend fun passFriend(user: UserEntity, targetUserId: Long) {
        interestMatchHistoryRepository.save(
            InterestMatchHistoryEntity(
                id = null,
                userId = user.id!!,
                targetId = targetUserId,
                status = "PASSED",
                createdAt = OffsetDateTime.now()
            )
        )
    }
}
