package kr.jiasoft.hiteen.admin.app

import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import kr.jiasoft.hiteen.admin.dto.AdminPointRuleCreateRequest
import kr.jiasoft.hiteen.admin.dto.AdminPointRuleResponse
import kr.jiasoft.hiteen.admin.dto.AdminPointRuleUpdateRequest
import kr.jiasoft.hiteen.admin.services.AdminPointRulesService
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
@RequestMapping("/api/admin/point-rules")
@Validated
class AdminPointRulesController(
    private val adminPointRulesService: AdminPointRulesService,
) {

    @GetMapping
    suspend fun list(
        @RequestParam status: String? = null,
        @RequestParam searchType: String? = null,
        @RequestParam search: String? = null,
        @RequestParam order: String = "ASC",
        @RequestParam size: Int = 10,
        @RequestParam page: Int = 1,
    ): ResponseEntity<ApiResult<ApiPage<AdminPointRuleResponse>>> {
        val data = adminPointRulesService.list(status, searchType, search, order, size, page)

        return success(data)
    }

    @GetMapping("/{id}")
    suspend fun get(
        @PathVariable id: Long,
        @RequestParam(required = false, defaultValue = "false") includeDeleted: Boolean = false,
    ): ResponseEntity<ApiResult<AdminPointRuleResponse>> {
        val data = adminPointRulesService.get(id, includeDeleted)
        return success(data)
    }

    @PostMapping
    suspend fun create(
        @Parameter @Valid @RequestBody request: AdminPointRuleCreateRequest,
    ): ResponseEntity<ApiResult<AdminPointRuleResponse>> {
        val data = adminPointRulesService.create(request)
        return success(data)
    }

    @PutMapping("/{id}")
    suspend fun update(
        @PathVariable id: Long,
        @Parameter @Valid @RequestBody request: AdminPointRuleUpdateRequest,
    ): ResponseEntity<ApiResult<AdminPointRuleResponse>> {
        val data = adminPointRulesService.update(id, request)
        return success(data)
    }

    @DeleteMapping("/{id}")
    suspend fun delete(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResult<Map<String, Any>>> {
        adminPointRulesService.delete(id)
        return success(mapOf("id" to id))
    }
}
