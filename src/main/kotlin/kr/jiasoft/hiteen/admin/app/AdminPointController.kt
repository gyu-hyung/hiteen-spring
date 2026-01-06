package kr.jiasoft.hiteen.admin.app

import io.swagger.v3.oas.annotations.Parameter
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminPointGiveRequest
import kr.jiasoft.hiteen.admin.dto.AdminPointResponse
import kr.jiasoft.hiteen.admin.dto.AdminUserSearchResponse
import kr.jiasoft.hiteen.admin.infra.AdminUserRepository
import kr.jiasoft.hiteen.admin.services.AdminPointService
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.interest.domain.InterestEntity
import kr.jiasoft.hiteen.feature.interest.dto.InterestRegisterRequest
import kr.jiasoft.hiteen.feature.point.domain.PointEntity
import kr.jiasoft.hiteen.feature.point.dto.PointChargeRequest
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/admin/point")
class AdminPointController(
    private val pointService: AdminPointService,
    private val adminUserRepository: AdminUserRepository,
) {
    // 포인트 목록
    @GetMapping("/points")
    suspend fun getPoints(
        @RequestParam type: String? = null,
        @RequestParam startDate: LocalDate? = null,
        @RequestParam endDate: LocalDate? = null,
        @RequestParam searchType: String? = null,
        @RequestParam search: String? = null,
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
    ): ResponseEntity<ApiResult<ApiPage<AdminPointResponse>>?> {
        val startDate = startDate?.atStartOfDay() // 2025-12-01 00:00:00
        val endDate = endDate?.plusDays(1)?.atStartOfDay() // 2025-12-30 00:00:00 (exclusive)

        val data = pointService.listPoints(
            type, startDate, endDate, searchType, search, page, size
        )

        return ResponseEntity.ok(ApiResult.success(data))
    }

    // 전체회원수
    @GetMapping("/users/total")
    suspend fun getUsersTotal(): ResponseEntity<ApiResult<Map<String, Long>>?> {
        val count = adminUserRepository.countByRoleAndDeletedAtIsNull("USER")
        return ResponseEntity.ok(
            ApiResult.success(
                mapOf("userCount" to count)
            )
        )
    }

    // 회원 검색
    @GetMapping("/users/search")
    suspend fun getUsersSearch(
        @RequestParam keyword: String? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<List<AdminUserSearchResponse>>?>? {
        val data = adminUserRepository.listSearchUsers(keyword).toList()
        return ResponseEntity.ok(ApiResult.success(data))
    }

    // 포인트 지급/차감 처리
    @PostMapping("/give")
    suspend fun givePoint(
        @Parameter request: AdminPointGiveRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) : ResponseEntity<ApiResult<PointEntity>?>? {
        val saved = pointService.givePoint(request)
        return ResponseEntity.ok(ApiResult.success(saved))
    }
}