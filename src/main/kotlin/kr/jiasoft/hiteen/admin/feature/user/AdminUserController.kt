package kr.jiasoft.hiteen.admin.feature.user

import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.user.app.UserService
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import kotlin.math.min

@RestController
@RequestMapping("/api/admin/user")
class AdminUserController (
    private val userService: UserService,
) {

    data class AdminUserResponse(
        val id: Long,
        val assetUid: String,
        val nickname: String,
        val username: String,
        val phone: String,
        val gender: String,
        val birthday: String,
        val schoolName: String,
        val locationMode: String,
        val point: String,
        val role: String,
        val createdAt: String,
        val accessedAt: String,
        val deletedAt: String,
    )

    @GetMapping("/users")
    suspend fun getUsers(
        @RequestParam("page", defaultValue = "1") pageParam: Int,
        @RequestParam("size", defaultValue = "0") sizeParam: Int,
        @RequestParam("nickname", defaultValue = "0") nickname: String? = null,
        @RequestParam("email", defaultValue = "0") email: String? = null,
        @RequestParam("phone", defaultValue = "0") phone: String? = null,
        @RequestParam("status", defaultValue = "0") status: String? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPage<AdminUserResponse>>> {
        println("nickname = ${nickname}")
        println("email = ${email}")
        println("phone = ${phone}")
        println("status = ${status}")

        val allUsers = listOf(
            AdminUserResponse(1, "131e00bf-2ded-4d80-bbfe-1cca502c2f63", "홍길동", "hong1234", "010-1234-5678", "남", "1999.01.01", "학력인정부산예원고등학교병설예원여자중학교(2년제)", "안개", "1000", "ADMIN", "2025-12-02 12:30", "2025-12-03 12:30", "2025-12-03 12:31"),
            AdminUserResponse(2, "131e00bf-2ded-4d80-bbfe-1cca502c2f63", "홍길동", "hong1234", "010-1234-5678", "남", "1999.01.01", "학력인정부산예원고등학교병설예원여자중학교(2년제)", "안개", "1000", "ADMIN", "2025-12-02 12:30", "2025-12-03 12:30", "2025-12-03 12:31"),
            AdminUserResponse(3, "131e00bf-2ded-4d80-bbfe-1cca502c2f63", "홍길동", "hong1234", "010-1234-5678", "남", "1999.01.01", "학력인정부산예원고등학교병설예원여자중학교(2년제)", "안개", "1000", "ADMIN", "2025-12-02 12:30", "2025-12-03 12:30", "2025-12-03 12:31"),
            AdminUserResponse(4, "131e00bf-2ded-4d80-bbfe-1cca502c2f63", "홍길동", "hong1234", "010-1234-5678", "남", "1999.01.01", "학력인정부산예원고등학교병설예원여자중학교(2년제)", "안개", "1000", "ADMIN", "2025-12-02 12:30", "2025-12-03 12:30", "2025-12-03 12:31"),
        )

        // 안전한 인자값 설정
        val perPage = if (sizeParam <= 0) 10 else sizeParam
        var page = if (pageParam <= 0) 1 else pageParam

        val total = allUsers.size

        // 총 페이지 수 계산 (0이면 0)
        val lastPage = if (total == 0) 0 else ((total + perPage - 1) / perPage)

        val startIndex = (page - 1) * perPage
        val endIndex = min(total, startIndex + perPage)

        val items = if (startIndex >= total || startIndex < 0) {
            emptyList()
        } else {
            allUsers.subList(startIndex, endIndex)
        }

        val result = ApiPage(
            total = total,
            lastPage = lastPage,
            items = items,
            perPage = perPage,
            currentPage = page,
        )

        return ResponseEntity.ok(ApiResult.success(result))
    }


}