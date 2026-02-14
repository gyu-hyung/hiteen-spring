package kr.jiasoft.hiteen.admin.app

import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import kr.jiasoft.hiteen.admin.dto.AdminCashRuleCreateRequest
import kr.jiasoft.hiteen.admin.dto.AdminCashRuleResponse
import kr.jiasoft.hiteen.admin.dto.AdminCashRuleUpdateRequest
import kr.jiasoft.hiteen.admin.services.AdminCashRulesService
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.extensions.success
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/cash-rules")
@Validated
class AdminCashRulesController(
    private val adminCashRulesService: AdminCashRulesService,
) {

    @GetMapping
    suspend fun list(
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "DESC",
        @RequestParam(required = false) search: String? = null,
        @RequestParam searchType: String = "ALL",
        /**
         * ACTIVE(기본): deleted_at IS NULL
         * DELETED: deleted_at IS NOT NULL
         * ALL: 전체
         */
        @RequestParam status: String? = null,
    ): ResponseEntity<ApiResult<ApiPage<AdminCashRuleResponse>>> {
        val data = adminCashRulesService.list(
            search = search,
            searchType = searchType,
            status = status,
            order = order,
            currentPage = page,
            perPage = size,
        )
        return success(data)
    }

    @GetMapping("/{id}")
    suspend fun get(
        @PathVariable id: Long,
        @RequestParam(required = false, defaultValue = "false") includeDeleted: Boolean = false,
    ): ResponseEntity<ApiResult<AdminCashRuleResponse>> {
        val data = adminCashRulesService.get(id, includeDeleted)
        return success(data)
    }

    @PostMapping
    suspend fun create(
        @Parameter @Valid @RequestBody request: AdminCashRuleCreateRequest,
    ): ResponseEntity<ApiResult<AdminCashRuleResponse>> {
        val data = adminCashRulesService.create(request)
        return success(data)
    }

    @PutMapping("/{id}")
    suspend fun update(
        @PathVariable id: Long,
        @Parameter @Valid @RequestBody request: AdminCashRuleUpdateRequest,
    ): ResponseEntity<ApiResult<AdminCashRuleResponse>> {
        val data = adminCashRulesService.update(id, request)
        return success(data)
    }

    @DeleteMapping("/{id}")
    suspend fun delete(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResult<Map<String, Any>>> {
        adminCashRulesService.delete(id)
        return success(mapOf("id" to id))
    }
}

