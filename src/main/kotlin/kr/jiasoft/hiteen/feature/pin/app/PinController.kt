package kr.jiasoft.hiteen.feature.pin.app

import PinResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.pin.dto.PinRegisterRequest
import kr.jiasoft.hiteen.feature.pin.dto.PinUpdateRequest
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "Pin", description = "지도 핀 관련 API")
@RestController
@RequestMapping("/api/pins")
@SecurityRequirement(name = "bearerAuth")   // 🔑 JWT 필요
class PinController(
    private val pinService: PinService
) {

    @Operation(
        summary = "지도 핀 목록 조회",
        description = "현재 위치(lat, lng)와 반경(radius) 기준으로 지도에서 볼 수 있는 핀들을 조회합니다. 기본 반경은 5km입니다."
    )
    @GetMapping
    suspend fun listVisiblePins(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "위도 (Latitude)") @RequestParam lat: Double,
        @Parameter(description = "경도 (Longitude)") @RequestParam lng: Double,
        @Parameter(description = "조회 반경 (미터 단위, 기본값 5000)") @RequestParam(required = false, defaultValue = "5000") radius: Double
    ): ResponseEntity<ApiResult<List<PinResponse>>> {
        val pins = pinService.listVisiblePins(user, lat, lng, radius)
        return ResponseEntity.ok(ApiResult.success(pins))
    }

    @Operation(summary = "내가 등록한 핀 목록", description = "현재 로그인한 사용자가 등록한 핀 목록을 조회합니다.")
    @GetMapping("/me")
    suspend fun myPins(
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ) = ResponseEntity.ok(ApiResult.success(pinService.listMyPins(user)))


    @Operation(summary = "핀 등록", description = "새로운 핀을 지도에 등록합니다.")
    @PostMapping
    suspend fun register(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "핀 등록 요청 DTO") pinRegisterRequest: PinRegisterRequest
    ) = ResponseEntity.ok(ApiResult.success(pinService.register(user, pinRegisterRequest)))


    @Operation(summary = "핀 수정", description = "기존 핀 정보를 수정합니다.")
    @PostMapping("/{id}")
    suspend fun update(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "핀 수정 요청 DTO") pinUpdateRequest: PinUpdateRequest
    ) = ResponseEntity.ok(ApiResult.success(pinService.update(user, pinUpdateRequest)))


    @Operation(summary = "핀 삭제", description = "특정 핀을 삭제합니다.")
    @DeleteMapping("/{id}")
    suspend fun delete(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "삭제할 핀 ID") @PathVariable id: Long
    ) = ResponseEntity.ok(ApiResult.success(pinService.delete(user, id)))
}
