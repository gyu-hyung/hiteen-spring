package kr.jiasoft.hiteen.admin.app

import io.swagger.v3.oas.annotations.Parameter
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminPointGiveRequest
import kr.jiasoft.hiteen.admin.dto.AdminPointResponse
import kr.jiasoft.hiteen.admin.dto.AdminUserSearchResponse
import kr.jiasoft.hiteen.admin.infra.AdminUserRepository
import kr.jiasoft.hiteen.admin.services.AdminPointService
import kr.jiasoft.hiteen.common.extensions.failure
import kr.jiasoft.hiteen.common.extensions.success
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

enum class PointType { CREDIT, DEBIT }

@RestController
@RequestMapping("/api/admin/point")
class AdminPointController(
    private val pointService: AdminPointService,
    private val userRepository: AdminUserRepository,
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
        @RequestParam uid: UUID? = null,
    ): ResponseEntity<ApiResult<ApiPage<AdminPointResponse>>> {
        val startDate = startDate?.atStartOfDay() // 2025-12-01 00:00:00
        val endDate = endDate?.plusDays(1)?.atStartOfDay() // 2025-12-30 00:00:00 (exclusive)

        val data = pointService.listPoints(
            type, startDate, endDate, searchType, search, page, size, uid
        )

        return success(data)
    }

    // 전체회원수
    @GetMapping("/users/total")
    suspend fun getUsersTotal(): ResponseEntity<ApiResult<Map<String, Long>>> {
        val count = userRepository.countByRoleAndDeletedAtIsNull("USER")
        val data = mapOf("userCount" to count)

        return success(data)
    }

    // 회원 검색
    @GetMapping("/users/search")
    suspend fun getUsersSearch(
        @RequestParam keyword: String? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<List<AdminUserSearchResponse>>> {
        val data = userRepository.listSearchUsers(keyword).toList()

        return success(data)
    }

    // 포인트 지급/차감 처리
    @PostMapping("/give")
    suspend fun give(
        @Parameter request: AdminPointGiveRequest,
    ) : ResponseEntity<ApiResult<Any>> {
        val type: PointType = request.type?.uppercase()?.let(PointType::valueOf) ?: PointType.CREDIT
        var point = request.point ?: 0
        var memo = request.memo ?: ""
        val receivers = request.receivers ?: emptyList()
        var targetType: String? = null

        if (point == 0) {
            return failure("포인트를 입력해 주세요.")
        }
        if (receivers.isEmpty()) {
            return failure("회원을 한 명 이상 선택해 주세요.")
        }

        when (type) {
            PointType.DEBIT -> {
                if (point > 0) point *= -1
                if (memo.isBlank()) memo = "운영자 차감"
            }
            PointType.CREDIT -> {
                if (point < 0) point *= -1
                if (memo.isBlank()) memo = "운영자 지급"
            }
        }

        for (phone in receivers) {
            if (phone == "all") {
                targetType = phone
                break
            }
        }

        val users: List<UserEntity> = when (targetType) {
            "all" -> userRepository.findByRole("USER")
            else -> {
                userRepository.findUsersByPhones(
                    role = "USER",
                    phones = receivers
                )
            }
        }

        val total = users.size
        if (total < 1) {
            return failure("회원을 한 명 이상 선택해 주세요.")
        }

        users.forEach { user ->
            pointService.givePoint(
                user.id,
                "ADMIN",
                null,
                type.name,
                point,
                memo
            )
        }

        val message = if (type == PointType.DEBIT) {
            "${total}명 회원의 포인트를 차감했습니다."
        } else {
            "${total}명 회원에게 포인트를 지급했습니다."
        }

        val data = mapOf("receivers" to total)

        return success(data, message)
    }
}