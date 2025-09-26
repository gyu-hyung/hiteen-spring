package kr.jiasoft.hiteen.feature.level.app

import kr.jiasoft.hiteen.feature.level.config.ExpProperties
import kr.jiasoft.hiteen.feature.level.domain.UserExpHistoryEntity
import kr.jiasoft.hiteen.feature.level.infra.TierRepository
import kr.jiasoft.hiteen.feature.level.infra.UserExpHistoryRepository
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ExpService(
    private val props: ExpProperties,
    private val userRepository: UserRepository,
    private val tierRepository: TierRepository,
    private val userExpHistoryRepository: UserExpHistoryRepository,
) {

    suspend fun grantExp(
        userId: Long,
        actionCode: String,
        targetId: Long,
        dynamicPoints: Int? = null
    ) {
        val action = props.actions[actionCode]
            ?: throw IllegalArgumentException("정의되지 않은 액션 코드: $actionCode")

        val points = dynamicPoints ?: action.points
        if (points == 0) return

        // 액션별 검증
        if (!validateAction(userId, actionCode, targetId, action.dailyLimit)) return

        // 유저 조회
        val user = userRepository.findById(userId) ?: return
        var newExp = (user.expPoints) + points

        // 티어 갱신
        val newTier = tierRepository.findByPoints(newExp)
        //레벨업 5점 추가
        if(newTier?.id != user.tierId) newExp += 5

        userRepository.updateExpAndTier(userId, newExp, newTier?.id)

        // 이력 기록
        userExpHistoryRepository.save(
            UserExpHistoryEntity(
                userId = userId,
                actionCode = actionCode,
                targetId = targetId,
                points = points,
                reason = action.description
            )
        )
    }

    /**
     * 액션별 검증 로직
     */
    private suspend fun validateAction(
        userId: Long,
        actionCode: String,
        targetId: Long?,
        dailyLimit: Int?
    ): Boolean {
        return when (actionCode) {
            "FRIEND_ADD" -> existTargetId(userId, actionCode, targetId!!, dailyLimit)
            "FRIEND_PROFILE_VISIT" -> existTargetIdAndToDay(userId, actionCode, targetId!!)
            "CHAT" -> existTargetIdAndToDay(userId, actionCode, targetId)
            "INTEREST_TAG_REGISTER" -> exists(userId, actionCode)
            "TINFRIEND_REQUEST" -> existTargetId(userId, actionCode, targetId!!, dailyLimit)
            "FOLLOW_REQUEST" -> existTargetId(userId, actionCode, targetId!!, dailyLimit)
            "FOLLOW_ACCEPT" -> existTargetId(userId, actionCode, targetId!!, dailyLimit)
            "LIKE_BOARD" -> existTargetId(userId, actionCode, targetId!!, dailyLimit)
            "LIKE_VOTE" -> existTargetId(userId, actionCode, targetId!!, dailyLimit)
            "LIKE_BOARD_COMMENT" -> existTargetId(userId, actionCode, targetId!!, dailyLimit)
            "LIKE_VOTE_COMMENT" -> existTargetId(userId, actionCode, targetId!!, dailyLimit)
            "FRIEND_INVITE" -> existTargetId(userId, actionCode, targetId!!, dailyLimit)
            else -> validateCommon(userId, actionCode, dailyLimit)
        }
    }


    /**
     * 공통: 일일 횟수 제한만 적용
     * dailyLimit 이 null 이면 무제한 허용
     */
    private suspend fun validateCommon(userId: Long, actionCode: String, dailyLimit: Int?): Boolean {
        if (dailyLimit != null) {
            val todayCount = userExpHistoryRepository.countToday(userId, actionCode, LocalDate.now())
            if (todayCount >= dailyLimit) return false
        }
        return true
    }


    private suspend fun exists(userId: Long, actionCode: String): Boolean {
        val exists = userExpHistoryRepository.exists(userId, actionCode)
        return !exists
    }

    private suspend fun existTargetId(userId: Long, actionCode: String, targetId: Long, dailyLimit: Int?): Boolean {
        val exists = userExpHistoryRepository.existsTargetId(userId, actionCode, targetId)
        if(exists) return false

        // 총 횟수 제한
        if (dailyLimit != null) {
            val count = userExpHistoryRepository.count(userId, actionCode)
            if (count >= dailyLimit) return false
        }
        return true
    }


    private suspend fun existTargetIdAndToDay(
        userId: Long,
        actionCode: String,
        targetId: Long?,
    ): Boolean {
        if (targetId == null) return false
        if (userId == targetId) return false

        val alreadyGiven = userExpHistoryRepository.existsTargetIdAndToday(userId, actionCode, targetId, LocalDate.now())
        return !alreadyGiven
    }


}

