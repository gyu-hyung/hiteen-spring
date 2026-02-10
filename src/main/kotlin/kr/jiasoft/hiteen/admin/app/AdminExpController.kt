package kr.jiasoft.hiteen.admin.app

import io.swagger.v3.oas.annotations.Parameter
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminExpGiveRequest
import kr.jiasoft.hiteen.admin.dto.AdminExpResponse
import kr.jiasoft.hiteen.admin.dto.AdminUserSearchResponse
import kr.jiasoft.hiteen.admin.infra.AdminUserRepository
import kr.jiasoft.hiteen.admin.services.AdminExpService
import kr.jiasoft.hiteen.common.extensions.failure
import kr.jiasoft.hiteen.common.extensions.success
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.level.app.ExpService
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

enum class ExpType { CREDIT, DEBIT }

@RestController
@RequestMapping("/api/admin/exp")
class AdminExpController(
    private val appExpService: ExpService,
    private val adminExpService: AdminExpService,
    private val adminUserRepository: AdminUserRepository,
) {
    // 경험치 목록
    @GetMapping("/list")
    suspend fun getExpHistory(
        @RequestParam status: String? = null,
        @RequestParam type: String? = null,
        @RequestParam startDate: LocalDate? = null,
        @RequestParam endDate: LocalDate? = null,
        @RequestParam searchType: String? = null,
        @RequestParam search: String? = null,
        @RequestParam uid: UUID? = null,
        @RequestParam page: Int = 1,
        @RequestParam perPage: Int = 10,
    ): ResponseEntity<ApiResult<ApiPage<AdminExpResponse>>> {
        val startDate = startDate?.atStartOfDay()
        val endDate = endDate?.plusDays(1)?.atStartOfDay()

        val data = adminExpService.expHistory(status, type, startDate, endDate, searchType, search, uid, page, perPage)

        return success(data)
    }

    // 전체회원수
    @GetMapping("/users/total")
    suspend fun getUsersTotal(): ResponseEntity<ApiResult<Map<String, Long>>> {
        val count = adminUserRepository.countByRoleAndDeletedAtIsNull("USER")
        val data = mapOf("userCount" to count)

        return success(data)
    }

    // 회원 검색
    @GetMapping("/users/search")
    suspend fun getUsersSearch(
        @RequestParam keyword: String? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<List<AdminUserSearchResponse>>> {
        val data = adminUserRepository.listSearchUsers(keyword).toList()

        return success(data)
    }

    // 경험치 지급/차감
    @PostMapping("/give")
    suspend fun give(
        @Parameter @RequestBody req: AdminExpGiveRequest,
    ) : ResponseEntity<ApiResult<Any>> {
        val type: ExpType = req.type?.uppercase()?.let {
            try { ExpType.valueOf(it) } catch (e: Exception) { ExpType.CREDIT }
        } ?: ExpType.CREDIT

        var amount = req.amount ?: 0
        var memo = req.memo ?: ""
        val receivers = req.receivers ?: emptyList()

        if (amount == 0) {
            return failure("경험치를 입력해 주세요.")
        }
        if (receivers.isEmpty()) {
            return failure("회원을 한 명 이상 선택해 주세요.")
        }

        when (type) {
            ExpType.DEBIT -> {
                if (amount > 0) amount *= -1
                if (memo.isBlank()) memo = "운영자 차감"
            }
            ExpType.CREDIT -> {
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
            "all" -> adminUserRepository.findByRole("USER")
            else -> adminUserRepository.findUsersByPhones(role = "USER", phones = receivers)
        }

        val total = users.size
        if (total < 1) {
            return failure("회원을 한 명 이상 선택해 주세요.")
        }

        users.forEach { user ->
            appExpService.grantExp(
                userId = user.id,
                actionCode = "ADMIN",
                targetId = null,
                requestId = null,
                dynamicPoints = amount,
                dynamicMemo = memo,
                giveAdmin = true
            )
        }

        val message = if (type == ExpType.DEBIT) {
            "경험치를 차감했습니다."
        } else {
            "경험치를 지급했습니다."
        }

        val data = mapOf("total" to total)

        return success(data, message)
    }
}