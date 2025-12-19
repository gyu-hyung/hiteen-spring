package kr.jiasoft.hiteen.admin.feature.app

import io.swagger.v3.oas.annotations.Operation
import kr.jiasoft.hiteen.admin.feature.dto.AdminSchoolResponse
import kr.jiasoft.hiteen.admin.feature.services.AdminSchoolService
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/school")
class AdminSchoolController(
    private val schoolService: AdminSchoolService,
) {
    @Operation(
        summary = "관리자 > 학교 목록",
        description = "시도, 학교구분, 키워드, 페이지, 페이지 크기 등의 옵션을 이용해 학교 목록을 조회합니다."
    )
    @GetMapping("/schools")
    suspend fun getSchools(
        @RequestParam sido: String? = null,
        @RequestParam type: Int? = null,
        @RequestParam searchType: String? = null,
        @RequestParam search: String? = null,
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10
    ): ResponseEntity<ApiResult<ApiPage<AdminSchoolResponse>>?> {
        val data = schoolService.listSchools(
            sido, type, searchType, search, page, size
        )

        return ResponseEntity.ok(ApiResult.Companion.success(data))
    }
}