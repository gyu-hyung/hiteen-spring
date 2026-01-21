package kr.jiasoft.hiteen.admin.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.location.app.LocationAppService
import kr.jiasoft.hiteen.feature.location.domain.LocationHistory
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Admin Location", description = "관리자 위치 조회 API")
@RestController
@RequestMapping("/api/admin/location")
@Validated
@SecurityRequirement(name = "bearerAuth")
class AdminLocationController(
    private val locationAppService: LocationAppService,
) {

    @Operation(summary = "사용자 최신 위치 조회(관리자)", description = "특정 사용자(userUid)의 가장 최신 위치를 조회합니다.")
    @GetMapping("/latest/{userUid}")
    suspend fun getLatest(
        @Parameter(description = "조회할 사용자 UID") @PathVariable userUid: String,
    ): ResponseEntity<ApiResult<LocationHistory?>> =
        ResponseEntity.ok(ApiResult.success(locationAppService.getLatest(userUid)))
}

