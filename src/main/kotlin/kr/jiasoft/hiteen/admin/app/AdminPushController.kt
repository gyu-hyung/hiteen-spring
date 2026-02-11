package kr.jiasoft.hiteen.admin.app

import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import kr.jiasoft.hiteen.admin.dto.AdminPushCreateRequest
import kr.jiasoft.hiteen.admin.dto.AdminPushDeleteResponse
import kr.jiasoft.hiteen.admin.dto.AdminPushDetailResponse
import kr.jiasoft.hiteen.admin.dto.AdminPushListResponse
import kr.jiasoft.hiteen.admin.services.AdminPushService
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.extensions.success
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/admin/push")
@Validated
class AdminPushController(
    private val adminPushService: AdminPushService,
) {
    @GetMapping
    suspend fun list(
        @RequestParam type: String? = null,
        @RequestParam status: String? = null,
        @RequestParam startDate: LocalDate? = null,
        @RequestParam endDate: LocalDate? = null,
        @RequestParam searchType: String? = null,
        @RequestParam search: String? = null,
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "DESC",
    ): ResponseEntity<ApiResult<ApiPage<AdminPushListResponse>>> {
        val startDate = startDate?.atStartOfDay()
        val endDate = endDate?.plusDays(1)?.atStartOfDay()

        val data = adminPushService.list(
            type, status, startDate, endDate, searchType, search, page, size, order
        )
        return success(data)
    }

    @GetMapping("/{pushId}")
    suspend fun get(
        @PathVariable pushId: Long,
        @RequestParam detailPage: Int = 1,
        @RequestParam detailSize: Int = 50,
        @RequestParam(required = false) success: String? = null,
    ): ResponseEntity<ApiResult<AdminPushDetailResponse>> {
        val data = adminPushService.get(pushId, detailPage, detailSize, success)
        return success(data)
    }

    @PostMapping
    suspend fun createAndSend(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter @Valid @RequestBody request: AdminPushCreateRequest,
    ): ResponseEntity<ApiResult<AdminPushListResponse>> {
        val data = adminPushService.createAndSend(user.id, request)
        return success(data)
    }

    @DeleteMapping("/{pushId}")
    suspend fun delete(
        @PathVariable pushId: Long,
    ): ResponseEntity<ApiResult<AdminPushDeleteResponse>> {
        val data = adminPushService.delete(pushId)
        return success(data)
    }
}

