package kr.jiasoft.hiteen.feature.location.app

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.location.domain.LocationHistory
import kr.jiasoft.hiteen.feature.location.dto.LocationRequest
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Tag(name = "Location", description = "위치 관련 API")
@RestController
@RequestMapping("/api/location")
@Validated
@SecurityRequirement(name = "bearerAuth")   // 🔑 JWT 인증 필요
class LocationController(
    private val locationAppService: LocationAppService,
    private val locationHub: LocationHub,
    private val objectMapper: ObjectMapper,
) {

    @Operation(
        summary = "위치 저장",
        description = "현재 사용자의 위치를 저장합니다."
    )
    @PostMapping
    suspend fun saveLocation(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "위치 저장 요청 DTO") @RequestBody locationRequest: LocationRequest
    ): ResponseEntity<ApiResult<LocationHistory>> {
        val history = locationAppService.saveLocation(user, locationRequest)

        // Broadcast payload
        val payload = objectMapper.writeValueAsString(
            mapOf(
                "type" to "location_updated",
                "data" to history,
            )
        )
        locationHub.publish(user.uid, payload)

        return ResponseEntity.ok(ApiResult.success(history))
    }

    @Operation(
        summary = "사용자의 최신 위치 조회",
        description = "특정 사용자(userUid)의 가장 최신 위치를 조회합니다."
    )
    @GetMapping("/latest/{userUid}")
    suspend fun getLatest(
        @Parameter(description = "조회할 사용자 UID") @PathVariable userUid: String
    ): ResponseEntity<ApiResult<LocationHistory?>> =
        ResponseEntity.ok(ApiResult.success(locationAppService.getLatest(userUid)))

    @Operation(
        summary = "내 위치 기록 조회",
        description = "로그인한 사용자의 위치 기록을 조회합니다."
    )
    @GetMapping("/my")
    suspend fun getMy(
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<ApiResult<List<LocationHistory>>> =
        ResponseEntity.ok(ApiResult.success(locationAppService.getMyLocations(user)))
}
