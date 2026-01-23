package kr.jiasoft.hiteen.admin.app

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminReportProcessRequest
import kr.jiasoft.hiteen.admin.dto.AdminReportRejectRequest
import kr.jiasoft.hiteen.admin.dto.AdminReportResponse
import kr.jiasoft.hiteen.admin.services.AdminReportService
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
class AdminReportController(
    private val adminReportService: AdminReportService,
) {

    @GetMapping
    suspend fun list(
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "DESC",
        @RequestParam status: Int? = null,
        @RequestParam type: String? = null,
        @RequestParam userUid: UUID? = null,
        @RequestParam targetUid: UUID? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPage<AdminReportResponse>>> {
        val list = adminReportService.listByPage(
            page = page,
            size = size,
            order = order,
            status = status,
            type = type,
            userUid = userUid,
            targetUid = targetUid,
        ).toList()

        val totalCount = adminReportService.totalCount(
            status = status,
            type = type,
            userUid = userUid,
            targetUid = targetUid,
        )

        return ResponseEntity.ok(ApiResult.success(PageUtil.of(list, totalCount, page, size)))
    }

    @GetMapping("/{id}")
    suspend fun detail(
        @PathVariable id: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<AdminReportResponse>> {
        val result = adminReportService.detail(id)
        return ResponseEntity.ok(ApiResult.success(result))
    }

    @PostMapping("/{id}/process")
    suspend fun process(
        @PathVariable id: Long,
        @RequestBody request: AdminReportProcessRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<AdminReportResponse>> {
        val result = adminReportService.process(id, request)
        return ResponseEntity.ok(ApiResult.success(result))
    }

    @PostMapping("/{id}/reject")
    suspend fun reject(
        @PathVariable id: Long,
        @RequestBody request: AdminReportRejectRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<AdminReportResponse>> {
        val result = adminReportService.reject(id, request)
        return ResponseEntity.ok(ApiResult.success(result))
    }
}
