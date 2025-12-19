package kr.jiasoft.hiteen.feature.point.app

import kotlinx.coroutines.reactor.awaitSingleOrNull
import kr.jiasoft.hiteen.common.context.DeltaContextHelper.addDeltaPoint
import kr.jiasoft.hiteen.common.exception.NotEnoughPointException
import kr.jiasoft.hiteen.feature.point.domain.PointEntity
import kr.jiasoft.hiteen.feature.point.domain.PointPolicy
import kr.jiasoft.hiteen.feature.point.infra.PointRepository
import kr.jiasoft.hiteen.feature.point.infra.PointRuleRepository
import kr.jiasoft.hiteen.feature.point.infra.PointSummaryRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PointService(
//    private val webClient: WebClient,
    private val pointRepository: PointRepository,
    private val pointSummaryRepository: PointSummaryRepository,
    private val pointRuleRepository: PointRuleRepository
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
     * 정책 기반 포인트 지급/차감 V1
     */
    suspend fun applyPolicy1(
        userId: Long,
        policy: PointPolicy,
        refId: Long? = null,
        dynamicPoint: Int? = null,
    ): PointEntity {
        // 1. 일일 제한 체크
        policy.dailyLimit?.let { limit ->
            val todayCount = pointRepository.countByUserAndPolicyAndDate(userId, policy.code, LocalDate.now())
            if (todayCount >= limit) {
//                throw IllegalStateException("오늘은 더 이상 '${policy.memoTemplate}' 포인트를 받을 수 없습니다. (일일 제한: $limit)")
                println("오늘은 더 이상 '${policy.memoTemplate}' 포인트를 받을 수 없습니다. (일일 제한: $limit)")
            }
        }
        val pointAmount = dynamicPoint ?: policy.amount
        // 2. 포인트 차감인 경우 보유 포인트 확인
        if (pointAmount < 0) {
            val totalPoints = getUserTotalPoints(userId)
            if (totalPoints < -pointAmount) {
                throw NotEnoughPointException("포인트가 부족합니다. (보유=${totalPoints}, 필요=${-pointAmount})")
            }
        }

        // 3. 기록 저장
        val point = PointEntity(
            userId = userId,
            point = pointAmount,
            type = if (pointAmount > 0) "CREDIT" else "DEBIT",
            pointableType = policy.code,
            pointableId = refId,
            memo = policy.memoTemplate
        )
        pointSummaryRepository.upsertAddPoint(userId, pointAmount)

        addDeltaPoint(pointAmount).awaitSingleOrNull()

        return pointRepository.save(point)
    }


    /**
     * 정책 기반 포인트 지급/차감 V2
     */
    suspend fun applyPolicy(
        userId: Long,
        pointPolicy: PointPolicy,
        refId: Long? = null,
        dynamicPoint: Int? = null
    ): PointEntity {

        // 1. 정책 조회
        val rule = pointRuleRepository.findActiveByActionCode(pointPolicy.code)
            ?: throw IllegalStateException("포인트 정책이 존재하지 않습니다. ($pointPolicy)")


        val pointAmount = dynamicPoint ?: rule.point

        // 2. 일일 제한
        rule.dailyCap?.let { cap ->
            val todayCount =
                pointRepository.countByUserAndPolicyAndDate(userId, pointPolicy.code, LocalDate.now())
            if (todayCount >= cap)
                println("오늘은 더 이상 '${rule.actionCode}' 포인트를 받을 수 없습니다. (일일 제한: $cap)")
//                throw IllegalStateException("일일 포인트 제한 초과 ($pointPolicy)")
        }

        // 3. 차감이면 잔액 확인
        if (pointAmount < 0) {
            val total = getUserTotalPoints(userId)
            if (total < -pointAmount) {
                throw NotEnoughPointException("포인트 부족")
            }
        }

        // 4. 포인트 기록
        val entity = PointEntity(
            userId = userId,
            point = pointAmount,
            type = if (pointAmount > 0) "CREDIT" else "DEBIT",
            pointableType = pointPolicy.code,
            pointableId = refId,
            memo = rule.description
        )

        pointSummaryRepository.upsertAddPoint(userId, pointAmount)
        addDeltaPoint(pointAmount).awaitSingleOrNull()

        return pointRepository.save(entity)
    }





//    suspend fun verifyPayment(paymentKey: String, orderId: String, amount: Int): Boolean {
//        return try {
//            val response = webClient.post()
//                .uri("https://api.tosspayments.com/v1/payments/confirm")
//                .header("Authorization", "Basic ${Base64.getEncoder().encodeToString("test_sk_xxx:".toByteArray())}")
//                .bodyValue(
//                    mapOf(
//                        "paymentKey" to paymentKey,
//                        "orderId" to orderId,
//                        "amount" to amount
//                    )
//                )
//                .retrieve()
//                .bodyToMono<Map<String, Any>>()
//                .awaitSingle()
//
//            response["status"] == "DONE" && (response["totalAmount"] as Int == amount)
//        } catch (e: Exception) {
//            false
//        }
//    }




}
