package kr.jiasoft.hiteen.admin.app

import kr.jiasoft.hiteen.admin.dto.AdminSmsDetailResponse
import kr.jiasoft.hiteen.admin.dto.AdminSmsListResponse
import kr.jiasoft.hiteen.admin.services.AdminSmsService
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.extensions.success
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/sms")
@Validated
class AdminSmsController(
    private val adminSmsService: AdminSmsService,
) {

    @GetMapping
    suspend fun list(
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 20,
        @RequestParam order: String = "DESC",
        @RequestParam(required = false) search: String? = null,
        @RequestParam searchType: String = "ALL", // ALL|PHONE|CONTENT|TITLE
    ): ResponseEntity<ApiResult<ApiPage<AdminSmsListResponse>>> {
        val data = adminSmsService.list(
            page = page,
            size = size,
            order = order,
            searchType = searchType,
            search = search,
        )
        return success(data)
    }

    @GetMapping("/{smsId}")
    suspend fun get(
        @PathVariable smsId: Long,
        @RequestParam authPage: Int = 1,
        @RequestParam authSize: Int = 50,
        @RequestParam(required = false) status: String? = null, // ALL|WAITING|VERIFIED...
        @RequestParam(required = false, defaultValue = "false") includeDeleted: Boolean = false,
    ): ResponseEntity<ApiResult<AdminSmsDetailResponse>> {
        val data = adminSmsService.getSmsWithAuthLogs(
            smsId = smsId,
            authPage = authPage,
            authSize = authSize,
            status = status,
            includeDeleted = includeDeleted,
        )
        return success(data)
    }
}

