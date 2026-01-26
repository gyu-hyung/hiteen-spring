package kr.jiasoft.hiteen.feature.ad.app

import io.r2dbc.postgresql.codec.Json
import kr.jiasoft.hiteen.feature.ad.domain.AdmobRewardEntity
import kr.jiasoft.hiteen.feature.ad.dto.AdRewardResult
import kr.jiasoft.hiteen.feature.ad.infra.AdmobRewardRepository
import kr.jiasoft.hiteen.feature.level.app.ExpService
import kr.jiasoft.hiteen.feature.point.app.PointService
import kr.jiasoft.hiteen.feature.point.domain.PointPolicy
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.stereotype.Service
import java.util.UUID


@Service
class AdService(
    private val admobRewardRepository: AdmobRewardRepository,
    private val pointService: PointService,
    private val expService: ExpService,
    private val userRepo: UserRepository,
) {

    // point_rules 설정이 없을 때만 하위호환 기본값
    private val DEFAULT_DAILY_AD_LIMIT = 5

    private suspend fun dailyAdLimit(): Int {
        return pointService.getDailyCapOrDefault(PointPolicy.AD_REWARD, DEFAULT_DAILY_AD_LIMIT)
    }

    /**
     * 광고 리워드 저장 + 포인트 지급, 보상 횟수 체크
     */
    private suspend fun saveRewardAndGrantPoint(
        transactionId: String,
        userId: Long,
        rawData: String? = null
    ): AdmobRewardEntity {
        if (admobRewardRepository.existsByTransactionId(transactionId))
            throw IllegalArgumentException("이미 처리된 트랜잭션 ID입니다.")

        val limit = dailyAdLimit()
        val todayCount = admobRewardRepository.countTodayByUserId(userId)
        if (todayCount >= limit)
            throw IllegalStateException("오늘은 광고 보상 횟수(최대 $limit 회)를 모두 사용했습니다.")

        // ✅ rawData(String?) → Json? 변환
        val jsonData = rawData?.let { Json.of(it) }

        val reward = admobRewardRepository.save(
            AdmobRewardEntity(
                transactionId = transactionId,
                userId = userId,
                reward = PointPolicy.AD_REWARD.amount,
                rawData = jsonData
            )
        )

        pointService.applyPolicy(userId, PointPolicy.AD_REWARD, reward.id)
        expService.grantExp(userId, "WATCH_AD", reward.id)

        return reward
    }


    /**
     * 광고 리워드 검증 및 포인트 지급 (차감 없음) + 남은 횟수 반환
     */
    suspend fun  verifyAdReward(
        transactionId: String,
        userId: String,
        rawData: String? = null
    ): AdRewardResult {

        val id = userRepo.findIdByUid(UUID.fromString(userId))
                ?: throw IllegalArgumentException("User not found")

        val reward = saveRewardAndGrantPoint(transactionId, id, rawData)
        val todayCount = admobRewardRepository.countTodayByUserId(id) // 저장 이후 카운트
        val remaining = dailyAdLimit() - todayCount
        return AdRewardResult(reward, remaining)
    }

    /**
     * 남은 광고 보기 수
     * */
    suspend fun getRemainingCount(userId: Long) : Int {
        val todayCount = admobRewardRepository.countTodayByUserId(userId)
        return  dailyAdLimit() - todayCount
    }

    /**
     * 광고 리워드 검증 + 게임 재도전 비용 차감
     */
    suspend fun verifyAdRewardAndUseForRetry(
        transactionId: String,
        userId: Long,
        gameId: Long? = null,
        rawData: String? = null,
    ) {
        saveRewardAndGrantPoint(transactionId, userId, rawData)

        // 4. 게임 재도전 비용 차감
        pointService.applyPolicy(userId, PointPolicy.GAME_PLAY, gameId)
    }
}
