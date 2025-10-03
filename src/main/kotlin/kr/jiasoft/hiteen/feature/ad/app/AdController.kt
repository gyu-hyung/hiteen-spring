package kr.jiasoft.hiteen.feature.ad.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.ad.dto.AdRewardRequest
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Admob", description = "광고 관련 API")
@RestController
@RequestMapping("/api/admob")
class AdController(
    private val adService: AdService
) {

    @Operation(summary = "남은 광고 보기 수 ")
    @GetMapping("/remaining")
    suspend fun getRemainingCount(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Int>> {
        return ResponseEntity.ok(ApiResult.success(adService.getRemainingCount(user.id)))
    }

    @Operation(summary = "광고 보기 보상")
    @PostMapping("/reward")
    suspend fun rewardByAd(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "광고 보상 요청 DTO") req: AdRewardRequest
    ): ResponseEntity<ApiResult<Unit>> {
        adService.verifyAdReward(
            transactionId = req.transactionId,
            userId = user.id,
            rawData = req.rawData
        )
        return ResponseEntity.ok(ApiResult.success())
    }

}