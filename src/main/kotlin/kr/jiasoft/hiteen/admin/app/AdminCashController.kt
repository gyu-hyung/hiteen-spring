package kr.jiasoft.hiteen.admin.app

import io.swagger.v3.oas.annotations.Parameter
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminCashGiveRequest
import kr.jiasoft.hiteen.admin.dto.AdminCashResponse
import kr.jiasoft.hiteen.admin.dto.AdminUserSearchResponse
import kr.jiasoft.hiteen.admin.infra.AdminUserRepository
import kr.jiasoft.hiteen.admin.services.AdminCashService
import kr.jiasoft.hiteen.common.extensions.failure
import kr.jiasoft.hiteen.common.extensions.success
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

enum class CashType { CREDIT, DEBIT }

@RestController
@RequestMapping("/api/admin/cash")
class AdminCashController(
    private val cashService: AdminCashService,
    private val userRepository: AdminUserRepository,
) {
    // 캐시 목록
    @GetMapping("/cash")
    suspend fun getCash(
        @RequestParam type: String? = null,
        @RequestParam status: String? = null,
        @RequestParam startDate: LocalDate? = null,
        @RequestParam endDate: LocalDate? = null,
        @RequestParam searchType: String? = null,
        @RequestParam search: String? = null,
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam uid: UUID? = null,
    ): ResponseEntity<ApiResult<ApiPage<AdminCashResponse>>> {
        val startDate = startDate?.atStartOfDay()
        val endDate = endDate?.plusDays(1)?.atStartOfDay()
        val search = search?.trim()?.takeIf { it.isNotBlank() }

        val data = cashService.listCash(type, status, startDate, endDate, searchType, search, page, size, uid)

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

    // 캐시 지급/차감 처리
    @PostMapping("/give")
    suspend fun give(
        @Parameter @RequestBody request: AdminCashGiveRequest,
    ) : ResponseEntity<ApiResult<Any>> {
        val type: CashType = request.type?.uppercase()?.let {
            try { CashType.valueOf(it) } catch (e: Exception) { CashType.CREDIT }
        } ?: CashType.CREDIT

        var amount = request.amount ?: 0
        var memo = request.memo ?: ""
        val receivers = request.receivers ?: emptyList()

        if (amount == 0) {
            return failure("금액을 입력해 주세요.")
        }
        if (receivers.isEmpty()) {
            return failure("회원을 한 명 이상 선택해 주세요.")
        }

        when (type) {
            CashType.DEBIT -> {
                if (amount > 0) amount *= -1
                if (memo.isBlank()) memo = "운영자 차감"
            }
            CashType.CREDIT -> {
                if (amount < 0) amount *= -1
                if (memo.isBlank()) memo = "운영자 지급"
            }
        }

        var targetType: String? = null
        for (phone in receivers) {
            if (phone == "all") {
                targetType = phone
                break
            }
        }

        val users: List<UserEntity> = when (targetType) {
            "all" -> userRepository.findByRole("USER")
            else -> userRepository.findUsersByPhones(role = "USER", phones = receivers)
        }

        val total = users.size
        if (total < 1) {
            return failure("회원을 한 명 이상 선택해 주세요.")
        }

        users.forEach { user ->
            cashService.giveCash(
                userId = user.id,
                cashableType = "ADMIN",
                cashableId = null,
                type = type.name,
                amount = amount,
                memo = memo
            )
        }

        val message = if (type == CashType.DEBIT) {
            "${total}명 회원의 캐시를 차감했습니다."
        } else {
            "${total}명 회원에게 캐시를 지급했습니다."
        }

        val data = mapOf("receivers" to total)

        return success(data, message)
    }
}
