package kr.jiasoft.hiteen.feature.point.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.level.app.ExpService
import kr.jiasoft.hiteen.feature.point.domain.PointEntity
import kr.jiasoft.hiteen.feature.point.domain.PointPolicy
import kr.jiasoft.hiteen.feature.point.dto.PointBalanceResponse
import kr.jiasoft.hiteen.feature.point.dto.PointChargeRequest
import kr.jiasoft.hiteen.feature.point.dto.PointSummary
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Tag(name = "Point", description = "포인트 관련 API")
@RestController
@RequestMapping("/api/points")
class PointController(
    private val pointService: PointService,
    private val expService: ExpService,
) {




    @Operation(summary = "내 포인트 이력 조회 (날짜 선택 가능)")
    @GetMapping("/me")
    suspend fun getMyPointHistory(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestParam(required = false) startDate: LocalDate?,
        @RequestParam(required = false) endDate: LocalDate?
    ): ResponseEntity<ApiResult<PointSummary>> {
        val total = pointService.getUserTotalPoints(user.id)
        val history = pointService.getUserPointHistory(user.id, startDate, endDate)
        return ResponseEntity.ok(ApiResult.success(PointSummary(
            total, history
        )))
    }

    @Operation(summary = "내 포인트 보유액 조회")
    @GetMapping("/balance")
    suspend fun getMyPointBalance(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<PointBalanceResponse>> {
        val total = pointService.getUserTotalPoints(user.id)
        return ResponseEntity.ok(ApiResult.success(PointBalanceResponse(total)))
    }

    @Operation(summary = "포인트 충전")
    @PostMapping("/charge")
    suspend fun chargePoints(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "포인트 충전 요청 DTO") req: PointChargeRequest
    ): ResponseEntity<ApiResult<PointEntity>> {
        val policy = when (req.amount) {
            1000 -> PointPolicy.PAYMENT_1000
            3000 -> PointPolicy.PAYMENT_3000
            5000 -> PointPolicy.PAYMENT_5000
            10000 -> PointPolicy.PAYMENT_10000
            else -> throw IllegalArgumentException("지원하지 않는 충전 금액: ${req.amount}")
        }
        val point = pointService.applyPolicy(user.id, policy, refId = req.paymentId.toLongOrNull())
        expService.grantExp(user.id, "POINT_CHARGE", point.id)
        return ResponseEntity.ok(ApiResult.success(point))
    }


}