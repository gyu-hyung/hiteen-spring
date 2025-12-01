package kr.jiasoft.hiteen.feature.terms.app

import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.terms.app.TermsService
import kr.jiasoft.hiteen.feature.terms.dto.TermsCreateRequest
import kr.jiasoft.hiteen.feature.terms.dto.TermsResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/terms")
class TermsController(
    private val service: TermsService
) {

    /** 약관 등록 */
    @PostMapping
    suspend fun create(@RequestBody req: TermsCreateRequest): ResponseEntity<ApiResult<TermsResponse>> =
        ResponseEntity.ok(ApiResult.success(service.create(req)))

    /**
     * 약관 목록 조회 (기본 Agreement 카테고리)
     * GET /api/terms?category=INQUIRY
     */
    @GetMapping
    suspend fun getAll(
        @RequestParam(required = false, defaultValue = "Agreement") category: String
    ): ResponseEntity<ApiResult<List<TermsResponse>>> =
        ResponseEntity.ok(ApiResult.success(service.getActiveList(category)))

    /**
     * 약관 상세 조회
     * plain=true → 기존 앱 호환 텍스트 버전
     */
    @GetMapping("/{uid}")
    suspend fun get(
        @PathVariable uid: UUID,
        @RequestParam(required = false, defaultValue = "false") plain: Boolean
    ): ResponseEntity<ApiResult<TermsResponse>> =
        ResponseEntity.ok(ApiResult.success(service.get(uid, plain)))
}
