package kr.jiasoft.hiteen.admin.app

import io.swagger.v3.oas.annotations.Operation
import kr.jiasoft.hiteen.admin.dto.AdminPointResponse
import kr.jiasoft.hiteen.admin.services.AdminPointService
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/admin/point")
class AdminPointController(
    private val pointService: AdminPointService,
) {
    @Operation(
        summary = "관리자 > 포인트 내역",
        description = "포인트 적립/차감 내역을 조회합니다."
    )
    @GetMapping("/points")
    suspend fun getPoints(
        @RequestParam type: String? = null,
        @RequestParam startDate: LocalDate? = null,
        @RequestParam endDate: LocalDate? = null,
        @RequestParam searchType: String? = null,
        @RequestParam search: String? = null,
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10
    ): ResponseEntity<ApiResult<ApiPage<AdminPointResponse>>?> {
        val startDate = startDate?.atStartOfDay() // 2025-12-01 00:00:00
        val endDate = endDate?.plusDays(1)?.atStartOfDay() // 2025-12-30 00:00:00 (exclusive)

        val data = pointService.listPoints(
            type, startDate, endDate, searchType, search, page, size
        )

        return ResponseEntity.ok(ApiResult.Companion.success(data))
    }
}