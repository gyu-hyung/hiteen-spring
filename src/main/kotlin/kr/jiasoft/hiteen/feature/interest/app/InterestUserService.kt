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
import kr.jiasoft.hiteen.feature.interest.infra.InterestRepository
import kr.jiasoft.hiteen.feature.interest.infra.InterestUserRepository
import kr.jiasoft.hiteen.feature.level.app.ExpService
import kr.jiasoft.hiteen.feature.location.infra.cache.LocationCacheRedisService
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
    private val locationCacheRedisService: LocationCacheRedisService,
    private val interestRepository: InterestRepository,
) {

    /**
     * 기본 관심사 등록: 추천옵션 (관심사, 남학생, 여학생, 동급생, 선배, 후배)
     */
    suspend fun initDefaultInterests(user: UserEntity) {
        // 기본 옵션 키워드
        val defaultOptions = listOf("관심사", "남학생", "여학생", "동급생", "선배", "후배")

        // ① 현재 등록된 관심사 조회
        val existing = interestUserRepository.findByUserIdWithInterest(user.id)
            .map { it.topic }
            .toSet()

        // ② 마스터 테이블에서 "추천옵션" 카테고리 중 기본 옵션에 해당하는 항목 조회
        val masterOptions = interestRepository.findByCategoryAndTopicIn("추천옵션", defaultOptions).toList()

        if (masterOptions.isEmpty()) {
            println("⚠️ 기본 관심사(추천옵션) 마스터 데이터가 존재하지 않습니다.")
            return
        }

        // ③ 등록되지 않은 항목만 필터링
        val toInsert = masterOptions.filterNot { existing.contains(it.topic) }
        if (toInsert.isEmpty()) {
            println("✅ 기본 추천옵션 관심사가 이미 모두 등록되어 있습니다.")
            return
        }

        // ④ interest_user 엔티티로 변환 후 저장
        toInsert.forEach { master ->
            interestUserRepository.save(
                InterestUserEntity(
                    interestId = master.id,
                    userId = user.id,
                )
            )
        }

        println("🌱 ${user.nickname ?: "유저"} 기본 추천옵션 관심사 ${toInsert.size}개 등록 완료")
    }


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
            var nearbyUserIds = emptySet<Long>()
            val radiusSteps = listOf(3.0, 10.0, 30.0) // km 단위 확장

            for (r in radiusSteps) {
                nearbyUserIds = locationCacheRedisService.findNearbyUserIds(user.uid.toString(), r)
                if (nearbyUserIds.isNotEmpty()) {
                    println("📍 반경 ${r}km 내 후보 발견: ${nearbyUserIds.size}명")
                    break
                }
            }

            if (nearbyUserIds.isNotEmpty()) {
                candidateUsers = candidateUsers.filter { nearbyUserIds.contains(it.id) }
            } else {
                println("⚠️ 반경 30km 내 후보 없음 → 거리 무시하고 전체 후보 유지")
            }
        }


        // 추천옵션 처리 (AND + OR 혼합)
        val userGrade = user.grade?.toIntOrNull() ?: 0

        candidateUsers = candidateUsers.filter { target ->
            val targetGrade = target.grade?.toIntOrNull() ?: 0

            // ✅ 성별 조건 (OR)
            val genderOk =
                when {
                    recommendOptions.contains("남학생") && recommendOptions.contains("여학생") -> true // 둘 다 선택시 모든 성별 허용
                    recommendOptions.contains("남학생") -> target.gender == "M"
                    recommendOptions.contains("여학생") -> target.gender == "F"
                    else -> true // 성별 조건 선택 안했으면 무시
                }

            // ✅ 학년 조건 (OR)
            val gradeOk =
                when {
                    listOf("동급생", "선배", "후배").none { recommendOptions.contains(it) } -> true // 학년 필터 미선택
                    else -> {
                        var ok = false
                        if (recommendOptions.contains("동급생") && targetGrade == userGrade) ok = true
                        if (recommendOptions.contains("선배") && targetGrade > userGrade) ok = true
                        if (recommendOptions.contains("후배") && targetGrade < userGrade) ok = true
                        ok
                    }
                }

            // ✅ 전체 조건 AND 결합
            genderOk && gradeOk
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

