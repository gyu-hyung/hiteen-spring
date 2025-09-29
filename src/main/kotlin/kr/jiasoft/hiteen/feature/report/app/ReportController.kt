package kr.jiasoft.hiteen.feature.report.app

import io.swagger.v3.oas.annotations.Operation
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.report.dto.ReportRequest
import kr.jiasoft.hiteen.feature.report.dto.ReportResponse
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/reports")
class ReportController(
    private val reportService: ReportService
) {

    @Operation(
        summary = "신고 등록",
        description = "로그인한 사용자가 특정 컨텐츠(댓글, 게시글 등)를 신고합니다."
    )
    @PostMapping
    suspend fun createReport(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        req: ReportRequest
    ): ResponseEntity<ApiResult<ReportResponse>> {
        val result = reportService.createReport(user.id, user.uid, req)
        return ResponseEntity.ok(ApiResult.success(result))
    }

    @Operation(
        summary = "내 신고 목록 조회",
        description = "로그인한 사용자가 지금까지 등록한 신고 내역을 조회합니다."
    )
    @GetMapping("/me")
    suspend fun getMyReports(
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<ApiResult<List<ReportResponse>>> {
        val result = reportService.getReportsByUser(user.id)
        return ResponseEntity.ok(ApiResult.success(result))
    }
}
