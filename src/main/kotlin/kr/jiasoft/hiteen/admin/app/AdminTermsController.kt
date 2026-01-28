package kr.jiasoft.hiteen.admin.app

import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.extensions.failure
import kr.jiasoft.hiteen.common.extensions.success
import kr.jiasoft.hiteen.feature.terms.app.TermsService
import kr.jiasoft.hiteen.feature.terms.dto.TermsCreateRequest
import kr.jiasoft.hiteen.feature.terms.dto.TermsResponse
import kr.jiasoft.hiteen.feature.terms.dto.TermsUpdateRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/admin/terms")
@PreAuthorize("hasRole('ADMIN')")
class AdminTermsController(
    private val termsService: TermsService,
) {

    @PostMapping
    suspend fun create(
        @Validated @RequestBody request: TermsCreateRequest,
    ): ResponseEntity<ApiResult<TermsResponse>> {
        return try {
            success(termsService.create(request))
        } catch (e: IllegalArgumentException) {
            failure(e.message ?: "요청이 올바르지 않습니다.")
        }
    }

    @PutMapping("/{uid}")
    suspend fun update(
        @PathVariable uid: UUID,
        @Validated @RequestBody request: TermsUpdateRequest,
    ): ResponseEntity<ApiResult<TermsResponse>> {
        return try {
            success(termsService.update(uid, request))
        } catch (e: IllegalArgumentException) {
            failure(e.message ?: "요청이 올바르지 않습니다.")
        }
    }

    @DeleteMapping("/{uid}")
    suspend fun delete(
        @PathVariable uid: UUID,
        @RequestParam deletedId: Long,
    ): ResponseEntity<ApiResult<Any>> {
        return try {
            termsService.delete(uid, deletedId)
            success(mapOf("uid" to uid))
        } catch (e: IllegalArgumentException) {
            failure(e.message ?: "요청이 올바르지 않습니다.")
        }
    }
}

