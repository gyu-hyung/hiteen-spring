package kr.jiasoft.hiteen.feature.point.app

import kotlinx.coroutines.reactor.awaitSingle
import kr.jiasoft.hiteen.feature.point.domain.PointEntity
import kr.jiasoft.hiteen.feature.point.domain.PointPolicy
import kr.jiasoft.hiteen.feature.point.infra.PointRepository
import kr.jiasoft.hiteen.feature.point.infra.PointSummaryRepository
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.LocalDate
import java.util.Base64

@Service
class PointService(
    private val webClient: WebClient,
    private val pointRepository: PointRepository,
    private val pointSummaryRepository: PointSummaryRepository
) {

    /**
     * 내 포인트 조회
     * */
    suspend fun getUserTotalPoints(userId: Long): Int {
        // summary 테이블 우선 조회, 없으면 fallback
        return pointSummaryRepository.findById(userId)?.totalPoint
            ?: pointRepository.sumPointsByUserId(userId) ?: 0
    }




    /**
     * 내포인트 이력
     * */
    suspend fun getUserPointHistory(
        userId: Long,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): List<PointEntity> {
        return if (startDate != null && endDate != null) {
            pointRepository.findAllByUserIdAndDateRange(userId, startDate, endDate)
        } else {
            pointRepository.findAllByUserId(userId)
        }
    }



    /**
     * 정책 기반 포인트 지급/차감
     */
    suspend fun applyPolicy(
        userId: Long,
        policy: PointPolicy,
        refId: Long? = null
    ): PointEntity {
        // 1. 일일 제한 체크
        policy.dailyLimit?.let { limit ->
            val todayCount = pointRepository.countByUserAndPolicyAndDate(userId, policy.code, LocalDate.now())
            if (todayCount >= limit) {
//                throw IllegalStateException("오늘은 더 이상 '${policy.memoTemplate}' 포인트를 받을 수 없습니다. (일일 제한: $limit)")
                println("오늘은 더 이상 '${policy.memoTemplate}' 포인트를 받을 수 없습니다. (일일 제한: $limit)")
            }
        }

        // 2. 포인트 차감인 경우 보유 포인트 확인
        if (policy.amount < 0) {
            val totalPoints = getUserTotalPoints(userId)
            if (totalPoints < -policy.amount) {
                throw IllegalStateException("포인트가 부족합니다. (보유=${totalPoints}, 필요=${-policy.amount})")
            }
        }

        // 3. 기록 저장
        val point = PointEntity(
            userId = userId,
            point = policy.amount,
            type = if (policy.amount > 0) "CREDIT" else "DEBIT",
            pointableType = policy.code,
            pointableId = refId,
            memo = policy.memoTemplate
        )
        pointSummaryRepository.upsertAddPoint(userId, policy.amount)
        return pointRepository.save(point)
    }




    suspend fun verifyPayment(paymentKey: String, orderId: String, amount: Int): Boolean {
        return try {
            val response = webClient.post()
                .uri("https://api.tosspayments.com/v1/payments/confirm")
                .header("Authorization", "Basic ${Base64.getEncoder().encodeToString("test_sk_xxx:".toByteArray())}")
                .bodyValue(
                    mapOf(
                        "paymentKey" to paymentKey,
                        "orderId" to orderId,
                        "amount" to amount
                    )
                )
                .retrieve()
                .bodyToMono<Map<String, Any>>()
                .awaitSingle()

            response["status"] == "DONE" && (response["totalAmount"] as Int == amount)
        } catch (e: Exception) {
            false
        }
    }




}
