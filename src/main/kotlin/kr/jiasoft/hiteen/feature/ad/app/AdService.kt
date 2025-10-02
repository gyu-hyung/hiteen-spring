package kr.jiasoft.hiteen.feature.ad.app

import kr.jiasoft.hiteen.feature.ad.domain.AdmobRewardEntity
import kr.jiasoft.hiteen.feature.ad.infra.AdmobRewardRepository
import kr.jiasoft.hiteen.feature.point.app.PointService
import org.springframework.stereotype.Service

@Service
class AdService(
    private val admobRewardRepository: AdmobRewardRepository,
    private val pointService: PointService
) {

    /**
     * 광고 리워드 검증 및 포인트 지급
     * - transactionId : 광고 SDK가 발급한 고유 트랜잭션
     * - userId        : 광고를 본 사용자
     * - rewardAmount  : 지급할 포인트
     * - rawData       : 원본 JSON (옵션, 추후 디버깅/검증용)
     */
    suspend fun verifyAdRewardAndUseForRetry(
        transactionId: String,
        userId: Long,
        rewardAmount: Int,
        retryCost: Int, // 재도전 기본 비용
        rawData: String? = null,
        gameId: Long? = null
    ) {
        // 1. 중복 체크 (이미 지급된 트랜잭션이면 무시)
        if (admobRewardRepository.existsByTransactionId(transactionId)) return

        // TODO: 실제 Google AdMob 서버 검증 (SSV) API 호출
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

        // 광고 리워드 기록 (PK id 사용 예정)
        val reward = admobRewardRepository.save(
            AdmobRewardEntity(
                transactionId = transactionId,
                userId = userId,
                reward = rewardAmount,
                rawData = rawData
            )
        )


        // 3. 포인트 적립
        pointService.addPoints(
            userId = userId,
            amount = rewardAmount,
            type = "AD",
            refType = "AD_REWARD",
            refId = reward.id, // 트랜잭션ID를 참조ID로 저장 가능
            memo = "[광고보기] ${rewardAmount}P 지급"
        )

        // 4. 재도전 차감
        pointService.usePoints(
            userId = userId,
            amount = retryCost,
            refType = "GAME_RETRY",
            refId = gameId,
            memo = "광고 재도전 (비용 ${retryCost}P 차감)"
        )

    }
}
