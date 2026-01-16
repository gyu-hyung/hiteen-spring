package kr.jiasoft.hiteen.admin.app

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminExpActionCreateRequest
import kr.jiasoft.hiteen.admin.dto.AdminExpActionResponse
import kr.jiasoft.hiteen.admin.dto.AdminExpActionUpdateRequest
import kr.jiasoft.hiteen.admin.services.AdminExpActionService
import kr.jiasoft.hiteen.common.extensions.failure
import kr.jiasoft.hiteen.common.extensions.success
import kr.jiasoft.hiteen.common.dto.ApiResult
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/exp-actions")
class AdminExpActionController(
    private val adminExpActionService: AdminExpActionService,
) {

    @GetMapping
    suspend fun list(
        @RequestParam enabled: Boolean? = null,
    ): ResponseEntity<ApiResult<List<AdminExpActionResponse>>> {
        val data = adminExpActionService.list(enabled).toList()
        return success(data)
    }

    @GetMapping("/{actionCode}")
    suspend fun get(
        @PathVariable actionCode: String,
    ): ResponseEntity<ApiResult<AdminExpActionResponse>> {
        val data = adminExpActionService.get(actionCode)
        return success(data)
    }

    @PostMapping
    suspend fun create(
        @Validated @RequestBody request: AdminExpActionCreateRequest,
    ): ResponseEntity<ApiResult<AdminExpActionResponse>> {
        return try {
            val data = adminExpActionService.create(request)
            success(data)
        } catch (e: IllegalArgumentException) {
            failure(e.message ?: "요청이 올바르지 않습니다.")
        }
    }

    @PutMapping("/{actionCode}")
    suspend fun update(
        @PathVariable actionCode: String,
        @Validated @RequestBody request: AdminExpActionUpdateRequest,
    ): ResponseEntity<ApiResult<AdminExpActionResponse>> {
        return try {
            val data = adminExpActionService.update(actionCode, request)
            success(data)
        } catch (e: IllegalArgumentException) {
            failure(e.message ?: "요청이 올바르지 않습니다.")
        }
    }

    @DeleteMapping("/{actionCode}")
    suspend fun disable(
        @PathVariable actionCode: String,
    ): ResponseEntity<ApiResult<Any>> {
        return try {
            adminExpActionService.disable(actionCode)
            success(mapOf("actionCode" to actionCode))
        } catch (e: IllegalArgumentException) {
            failure(e.message ?: "요청이 올바르지 않습니다.")
        }
    }
}

