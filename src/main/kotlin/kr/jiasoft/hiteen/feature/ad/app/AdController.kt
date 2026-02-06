package kr.jiasoft.hiteen.feature.ad.app

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.level.app.ExpService
import kr.jiasoft.hiteen.feature.point.app.PointService
import kr.jiasoft.hiteen.feature.point.domain.PointPolicy
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange

@Tag(name = "Admob", description = "광고 관련 API")
@RestController
@RequestMapping("/api/admob")
class AdController(
    private val adService: AdService,
    private val objectMapper: ObjectMapper,
    private val admobVerifier: AdmobVerifier,

    private val pointService: PointService,
    private val expService: ExpService,
) {

    @Operation(summary = "남은 광고 보기 수")
    @GetMapping("/remaining")
    suspend fun getRemainingCount(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Int>> {
        return ResponseEntity.ok(ApiResult.success(adService.getRemainingCount(user.id)))
    }

    @Operation(summary = "테스트용")
    @GetMapping("/earn")
    suspend fun earn(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<String>> {

        val available = pointService.validateDailyCap(user.id, PointPolicy.AD_REWARD)
        if (!available) {
            return ResponseEntity.badRequest().body(ApiResult.failure("오늘은 광고를 모두 시청했어!"))
        }

        pointService.applyPolicy(user.id, PointPolicy.AD_REWARD)
        expService.grantExp(user.id, "WATCH_AD")

        return ResponseEntity.ok(ApiResult.success("통과"))
    }

    @Operation(summary = "AdMob 서버 리워드 콜백 (SSV)")
    @GetMapping("/callback")
    suspend fun admobCallback(
        @RequestParam("transaction_id") transactionId: String?,
        @RequestParam("user_id") userId: String?,
        @RequestParam("reward_amount") rewardAmount: String?,
        @RequestParam("signature") signature: String?,
        @RequestParam("key_id") keyId: String?,
        @RequestParam(required = false, name = "custom_data") customData: String?,
        exchange: ServerWebExchange
    ): ResponseEntity<ApiResult<Any>> {
        val params = exchange.request.queryParams.toSingleValueMap()

        return try {
            // ✅ 1️⃣ SSV 서명 검증
            if (!admobVerifier.verifySignature(params, signature, keyId)) {
                throw IllegalArgumentException("AdMob SSV 서명 검증 실패")
            }

            // ✅ 2️⃣ 유효성 검사
            if (transactionId.isNullOrBlank() || userId == null) {
                throw IllegalArgumentException("잘못된 요청입니다.")
            }

            // ✅ 3️⃣ 로그/원본 저장
            val rawJson = objectMapper.writeValueAsString(params)

            // ✅ 4️⃣ 포인트/경험치 처리
            adService.verifyAdReward(transactionId, userId, rawJson)

            ResponseEntity.ok(ApiResult.success(true))
        } catch (ex: Exception) {
            ex.printStackTrace()
            ResponseEntity.badRequest().body(ApiResult.failure("AdMob callback failed"))
        }
    }

}
