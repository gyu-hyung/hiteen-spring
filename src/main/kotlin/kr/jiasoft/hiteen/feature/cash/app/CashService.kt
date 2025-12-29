package kr.jiasoft.hiteen.feature.cash.app

import kotlinx.coroutines.reactor.awaitSingleOrNull
import kr.jiasoft.hiteen.common.context.DeltaContextHelper.addDeltaCash
import kr.jiasoft.hiteen.common.context.DeltaContextHelper.addDeltaPoint
import kr.jiasoft.hiteen.common.exception.NotEnoughPointException
import kr.jiasoft.hiteen.feature.cash.domain.CashEntity
import kr.jiasoft.hiteen.feature.cash.domain.CashPolicy
import kr.jiasoft.hiteen.feature.cash.infra.CashRepository
import kr.jiasoft.hiteen.feature.cash.infra.CashRuleRepository
import kr.jiasoft.hiteen.feature.cash.infra.CashSummaryRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class CashService(
//    private val webClient: WebClient,
    private val cashRepository: CashRepository,
    private val cashSummaryRepository: CashSummaryRepository,
    private val cashRuleRepository: CashRuleRepository
) {

    /**
     * 내 포인트 조회
     * */
    suspend fun getUserTotalPoints(userId: Long): Int {
        // summary 테이블 우선 조회, 없으면 fallback
        return cashSummaryRepository.findById(userId)?.totalCash
            ?: cashRepository.sumCashByUserId(userId) ?: 0
    }




    /**
     * 내포인트 이력
     * */
    suspend fun getUserPointHistory(
        userId: Long,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): List<CashEntity> {
        return if (startDate != null && endDate != null) {
            cashRepository.findAllByUserIdAndDateRange(userId, startDate, endDate)
        } else {
            cashRepository.findAllByUserId(userId)
        }
    }



    /**
     * 정책 기반 포인트 지급/차감 V1
     */
    suspend fun applyPolicy1(
        userId: Long,
        policy: CashPolicy,
        refId: Long? = null,
        dynamicPoint: Int? = null,
    ): CashEntity {
        // 1. 일일 제한 체크
        policy.dailyLimit?.let { limit ->
            val todayCount = cashRepository.countByUserAndPolicyAndDate(userId, policy.code, LocalDate.now())
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
        val point = CashEntity(
            userId = userId,
            amount = pointAmount,
            type = if (pointAmount > 0) "CREDIT" else "DEBIT",
            cashableType = policy.code,
            cashableId = refId,
            memo = policy.memoTemplate
        )
        cashSummaryRepository.upsertAddPoint(userId, pointAmount)

        addDeltaPoint(pointAmount).awaitSingleOrNull()

        return cashRepository.save(point)
    }


    /**
     * 정책 기반 포인트 지급/차감 V2
     */
    suspend fun applyPolicy(
        userId: Long,
        cashPolicy: CashPolicy,
        refId: Long? = null,
        dynamicCash: Int? = null,
        description: String? = null,
    ): CashEntity {

        // 1. 정책 조회
        val rule = cashRuleRepository.findActiveByActionCode(cashPolicy.code)
            ?: throw IllegalStateException("포인트 정책이 존재하지 않습니다. ($cashPolicy)")


        val cashAmount = dynamicCash ?: rule.amount

        // 2. 일일 제한
        rule.dailyCap?.let { cap ->
            val todayCount =
                cashRepository.countByUserAndPolicyAndDate(userId, cashPolicy.code, LocalDate.now())
            if (todayCount >= cap)
                println("오늘은 더 이상 '${rule.actionCode}' 포인트를 받을 수 없습니다. (일일 제한: $cap)")
//                throw IllegalStateException("일일 포인트 제한 초과 ($pointPolicy)")
        }

        // 3. 차감이면 잔액 확인
        if (cashAmount < 0) {
            val total = getUserTotalPoints(userId)
            if (total < -cashAmount) {
                throw NotEnoughPointException("포인트 부족")
            }
        }

        // 4. 포인트 기록
        val entity = CashEntity(
            userId = userId,
            amount = cashAmount,
            type = if (cashAmount > 0) "CREDIT" else "DEBIT",
            cashableType = cashPolicy.code,
            cashableId = refId,
            memo = description?: rule.description
        )

        cashSummaryRepository.upsertAddPoint(userId, cashAmount)
        addDeltaCash(cashAmount).awaitSingleOrNull()

        return cashRepository.save(entity)
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
