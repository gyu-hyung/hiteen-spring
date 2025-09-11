package kr.jiasoft.hiteen.feature.interest.app

import kr.jiasoft.hiteen.feature.interest.domain.InterestUserEntity
import kr.jiasoft.hiteen.feature.interest.dto.InterestUserResponse
import kr.jiasoft.hiteen.feature.interest.infra.InterestUserRepository
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class InterestUserService(
    private val interestUserRepository: InterestUserRepository,
    private val userRepository: UserRepository
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
        return interestUserRepository.getInterestResponseById(null, userEntity?.id!!)
    }

    /** 특정 관심사 삭제 */
    suspend fun removeInterestFromUser(userId: Long, interestId: Long): Boolean {
        val deletedCount = interestUserRepository.deleteByUserIdAndInterestId(userId, interestId)
        return deletedCount > 0
    }

    /** 특정 사용자의 모든 관심사 초기화 */
    suspend fun clearUserInterests(userId: Long) {
        interestUserRepository.findByUserId(userId).forEach { interestUserRepository.delete(it) }
    }
}
