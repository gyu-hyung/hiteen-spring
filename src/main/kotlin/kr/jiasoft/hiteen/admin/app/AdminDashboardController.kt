package kr.jiasoft.hiteen.admin.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.extensions.success
import kr.jiasoft.hiteen.feature.dashboard.app.DashboardService
import kr.jiasoft.hiteen.feature.dashboard.dto.DashboardResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Admin Dashboard", description = "관리자 대시보드 API")
@RestController
@RequestMapping("/api/admin/dashboard")
class AdminDashboardController(
    private val dashboardService: DashboardService
) {

    @Operation(summary = "대시보드 통계 조회")
    @GetMapping
    suspend fun getDashboardData(): ResponseEntity<ApiResult<DashboardResponse>> {
        return success(dashboardService.getDashboardData())
    }

    @Operation(summary = "통계 수동 갱신 (관리자용)")
    @PostMapping("/refresh")
    suspend fun refreshStatistics(): ResponseEntity<ApiResult<Unit>> {
        dashboardService.refreshStatistics()
        return success(Unit)
    }
}

