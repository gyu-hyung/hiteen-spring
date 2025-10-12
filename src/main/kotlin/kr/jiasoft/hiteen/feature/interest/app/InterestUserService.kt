package kr.jiasoft.hiteen.feature.interest.app

import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.common.exception.BusinessValidationException
import kr.jiasoft.hiteen.feature.contact.infra.UserContactRepository
import kr.jiasoft.hiteen.feature.interest.domain.InterestMatchHistoryEntity
import kr.jiasoft.hiteen.feature.interest.domain.InterestUserEntity
import kr.jiasoft.hiteen.feature.interest.dto.FriendRecommendationResponse
import kr.jiasoft.hiteen.feature.interest.dto.InterestUserResponse
import kr.jiasoft.hiteen.feature.interest.infra.InterestMatchHistoryRepository
import kr.jiasoft.hiteen.feature.interest.infra.InterestUserRepository
import kr.jiasoft.hiteen.feature.level.app.ExpService
import kr.jiasoft.hiteen.feature.location.infra.cache.LocationCacheRedisService
import kr.jiasoft.hiteen.feature.point.app.PointService
import kr.jiasoft.hiteen.feature.point.domain.PointPolicy
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
    private val schoolRepository: SchoolRepository,
    private val userContactRepository: UserContactRepository,

    private val expService: ExpService,
    private val pointService: PointService,
    private val locationCacheRedisService: LocationCacheRedisService,
) {

    /** 특정 사용자 관심사 등록 */
    suspend fun addInterestToUser(user: UserEntity, interestId: Long): InterestUserResponse? {
        val exist = interestUserRepository.findByUserIdAndInterestId(user.id, interestId)
        if (exist != null) {
            return interestUserRepository.getInterestResponseById(exist.id, null).firstOrNull()
        }

        // 관심사 5개 이상 등록 불가 ('추천방식', '추천옵션', '추천제외' 제외)
        interestUserRepository.findByUserIdAndNotInSystemCategory(user.id).count()
            .takeIf { it >= 5 }
            ?.let { throw BusinessValidationException(mapOf("message" to "관심사가 5개를 초과했습니다.")) }

        val entity = InterestUserEntity(
            interestId = interestId,
            userId = user.id,
            createdAt = OffsetDateTime.now()
        )
        val saved = interestUserRepository.save(entity)
        expService.grantExp(user.id, "INTEREST_TAG_REGISTER", interestId)
        return interestUserRepository.getInterestResponseById(saved.id, null).firstOrNull()
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

        // 하루 추천 제한 확인
        val todayCount = interestMatchHistoryRepository.countTodayRecommendations(user.id)
        if (todayCount >= dailyLimit) {
            throw BusinessValidationException(mapOf("message" to "오늘은 추천 친구를 더 뽑을 수 없습니다."))
        }

        // 내 관심사 조회
        val myInterestEntities = interestUserRepository.findByUserIdWithInterest(user.id).toList()
        if (myInterestEntities.isEmpty()) {
            throw BusinessValidationException(mapOf("message" to "관심사가 존재하지 않습니다. 관심사를 추가해주세요~"))
        }

        // 분류별 분리
        val interestIds = myInterestEntities.filter { it.category !in listOf("추천방식", "추천옵션", "추천제외") }
            .map { it.id }
            .toSet()

        if (interestIds.isEmpty()) throw BusinessValidationException(mapOf("message" to "관심사가 존재하지 않습니다. 관심사를 추가해주세요~"))


        // 추천방식 [거리]
        val recommendMethods = myInterestEntities.filter { it.category == "추천방식" }.map { it.topic }
        // 추천옵션 [관심사, 남자, 여자, 동급생, 선배, 후배]
        val recommendOptions = myInterestEntities.filter { it.category == "추천옵션" }.map { it.topic }
        // 추천제외 [같은학교, 연락처]
        val recommendExcludes = myInterestEntities.filter { it.category == "추천제외" }.map { it.topic }


        // 후보 데이터 한번에 조회 (N+1 제거)
        var candidateUsers = interestUserRepository.findAvailableUsersWithProfileByInterestIds(interestIds, user.id).toList()
        if (candidateUsers.isEmpty()) return null

        // 추천방식 처리
        if (recommendMethods.contains("거리")) {
            val nearbyUserIds = locationCacheRedisService.findNearbyUserIds(user.uid.toString(), 5.0)
            if (nearbyUserIds.isNotEmpty()) {
                // ✅ 반경 내 후보가 존재하면 우선 거리 기반 추천만 유지
                candidateUsers = candidateUsers.filter { nearbyUserIds.contains(it.id) }
                println("📍 거리 기반 후보 ${nearbyUserIds.size}명")
            }
        }

        // 추천옵션 처리
        val userGrade = user.grade?.toIntOrNull() ?: 0
        candidateUsers = candidateUsers.filter { target ->
            var match = true
            if (recommendOptions.contains("남학생")) match = match && target.gender == "M"
            if (recommendOptions.contains("여학생")) match = match && target.gender == "F"

            val targetGrade = target.grade?.toIntOrNull() ?: 0
            if (recommendOptions.contains("동급생")) match = match && targetGrade == userGrade
            if (recommendOptions.contains("선배")) match = match && targetGrade > userGrade
            if (recommendOptions.contains("후배")) match = match && targetGrade < userGrade

            match
        }

        // 추천제외 처리
        if (recommendExcludes.contains("같은 학교") && user.schoolId != null) {
            candidateUsers = candidateUsers.filterNot { it.schoolId == user.schoolId }
        }
        if (recommendExcludes.contains("연락처")) {
            // ① 내가 등록한 연락처 목록 가져오기
            val myContactPhones = userContactRepository.findPhonesByUserId(user.id).toList().toSet()
            if (myContactPhones.isNotEmpty()) {
                // ② 연락처에 등록된 번호를 가진 사용자 조회
                val contactUsers = userRepository.findAllByPhoneIn(myContactPhones).toList()
                val contactUserIds = contactUsers.map { it.id }.toSet()

                // ③ 후보 목록에서 제외
                candidateUsers = candidateUsers.filterNot { contactUserIds.contains(it.id) }
            }
        }


        val targetUser = candidateUsers.randomOrNull() ?: return null
        val fullUser = userRepository.findById(targetUser.id) ?: return null
        val school = fullUser.schoolId?.let { schoolRepository.findById(it) }
        val targetUserResponse = UserResponse.from(fullUser, school)

        val interests = interestUserRepository.getInterestResponseById(null, targetUser.id).toList()
        val photos = userPhotosRepository.findByUserId(targetUser.id)?.toList() ?: emptyList()

        interestMatchHistoryRepository.save(
            InterestMatchHistoryEntity(
                userId = user.id,
                targetId = targetUser.id,
                status = "RECOMMENDED",
                createdAt = OffsetDateTime.now(),
            )
        )

        expService.grantExp(user.id, "TODAY_FRIEND_CHECK", targetUser.id)
        pointService.applyPolicy(user.id, PointPolicy.FRIEND_RECOMMEND)

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
                userId = user.id,
                targetId = targetUserId,
                status = "PASSED",
                createdAt = OffsetDateTime.now()
            )
        )
    }
}
