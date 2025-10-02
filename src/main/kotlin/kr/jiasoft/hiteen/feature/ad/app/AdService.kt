package kr.jiasoft.hiteen.feature.ad.app

import kr.jiasoft.hiteen.feature.ad.domain.AdmobRewardEntity
import kr.jiasoft.hiteen.feature.ad.infra.AdmobRewardRepository
import kr.jiasoft.hiteen.feature.level.app.ExpService
import kr.jiasoft.hiteen.feature.point.app.PointService
import kr.jiasoft.hiteen.feature.point.domain.PointPolicy
import org.springframework.stereotype.Service


@Service
class AdService(
    private val admobRewardRepository: AdmobRewardRepository,
    private val pointService: PointService,
    private val expService: ExpService
) {

    private val DAILY_AD_LIMIT = 5

    /**
     * 광고 리워드 저장 + 포인트 지급 공통 처리
     */
    private suspend fun saveRewardAndGrantPoint(
        transactionId: String,
        userId: Long,
        rewardAmount: Int,
        rawData: String? = null
    ): AdmobRewardEntity? {

        // 0. 이미 같은 트랜잭션이 처리됐으면 무시
        if (admobRewardRepository.existsByTransactionId(transactionId)) return null

        // 1. 오늘 지급된 광고 보상 횟수 체크
        val todayCount = admobRewardRepository.countTodayByUserId(userId)
        if (todayCount >= DAILY_AD_LIMIT) {
            throw IllegalStateException("오늘은 광고 보상 횟수(최대 $DAILY_AD_LIMIT 회)를 모두 사용했습니다.")
        }

        // TODO: 2.실제 Google AdMob 서버 검증 (SSV) API 호출
        // try {
        //     val response = httpClient.post("https://www.google.com/admob/ssv/verify") {
        //         parameter("transaction_id", transactionId)
        //         parameter("user_id", userId)
        //         parameter("reward_amount", rewardAmount)
        //     }
        //     if (!response.isSuccessful) {
        //         throw IllegalStateException("광고 검증 실패: $transactionId")
        //     }
        // } catch (ex: Exception) {
        //     throw IllegalStateException("광고 검증 중 오류 발생", ex)
        // }

        // 3. 광고 리워드 기록
        val reward = admobRewardRepository.save(
            AdmobRewardEntity(
                transactionId = transactionId,
                userId = userId,
                reward = rewardAmount,
                rawData = rawData
            )
        )

        // 4. 광고 포인트/경험치 지급
        pointService.applyPolicy(userId, PointPolicy.AD_REWARD, reward.id)
        expService.grantExp(userId, "WATCH_AD", reward.id)

        return reward
    }

    /**
     * 광고 리워드 검증 및 포인트 지급 (차감 없음)
     */
    suspend fun verifyAdReward(
        transactionId: String,
        userId: Long,
        rewardAmount: Int,
        rawData: String? = null
    ) {
        saveRewardAndGrantPoint(transactionId, userId, rewardAmount, rawData)
    }

    /**
     * 광고 리워드 검증 + 게임 재도전 비용 차감
     */
    suspend fun verifyAdRewardAndUseForRetry(
        transactionId: String,
        userId: Long,
        rewardAmount: Int,
        rawData: String? = null,
        gameId: Long? = null
    ) {
        saveRewardAndGrantPoint(transactionId, userId, rewardAmount, rawData)
            ?: return // 이미 지급된 트랜잭션이면 차감도 안 함

        // 4. 게임 재도전 비용 차감
        pointService.applyPolicy(userId, PointPolicy.GAME_PLAY, gameId)
    }
}

