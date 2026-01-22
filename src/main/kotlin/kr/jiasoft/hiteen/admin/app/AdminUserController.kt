package kr.jiasoft.hiteen.admin.app

import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminFollowResponse
import kr.jiasoft.hiteen.admin.dto.AdminFriendResponse
import kr.jiasoft.hiteen.admin.dto.AdminMyPasswordChangeRequest
import kr.jiasoft.hiteen.admin.dto.AdminUserResponse
import kr.jiasoft.hiteen.admin.dto.AdminUserRoleUpdateRequest
import kr.jiasoft.hiteen.admin.dto.AdminUserRoleUpdateResponse
import kr.jiasoft.hiteen.admin.dto.AdminUserSaveRequest
import kr.jiasoft.hiteen.admin.dto.AdminUserSearchResponse
import kr.jiasoft.hiteen.admin.infra.AdminFollowRepository
import kr.jiasoft.hiteen.admin.infra.AdminFriendRepository
import kr.jiasoft.hiteen.admin.infra.AdminUserRepository
import kr.jiasoft.hiteen.admin.services.AdminUserService
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.dto.ApiPageCursor
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.common.extensions.failure
import kr.jiasoft.hiteen.common.extensions.success
import kr.jiasoft.hiteen.feature.cash.app.CashService
import kr.jiasoft.hiteen.feature.point.app.PointService
import kr.jiasoft.hiteen.feature.user.app.UserService
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.IllegalArgumentException

@RestController
@RequestMapping("/api/admin/user")
class AdminUserController (
    private val userService: UserService,
    private val adminUserRepository: AdminUserRepository,
    private val adminFriendRepository: AdminFriendRepository,
    private val adminFollowRepository: AdminFollowRepository,
    private val encoder: PasswordEncoder,
    private val adminUserService: AdminUserService,
    private val pointService: PointService,
    private val cashService: CashService,
) {

    // 회원 검색 (role 조건 없음)
    @GetMapping("/users/search")
    suspend fun getUsersSearch(
        @RequestParam keyword: String? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<List<AdminUserSearchResponse>>> {
        val data = adminUserRepository.listSearchUsersAllRoles(keyword).toList()
        return ResponseEntity.ok(ApiResult.success(data))
    }

    @GetMapping("/users")
    suspend fun getUsers(
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "DESC",
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam status: String? = null,
        @RequestParam id: Long? = null,
        @RequestParam uid: String? = null,
        @RequestParam role: String? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPage<AdminUserResponse>>> {

        val res = adminUserRepository.listByPage(
            page = page,
            size = size,
            order = order,
            search = search,
            searchType = searchType,
            status = status,
            role = role
        ).toList()

        val totalCount = adminUserRepository.totalCount(
            search = search,
            searchType = searchType,
            status = status,
            role = role
        )

        return ResponseEntity.ok(ApiResult.success(PageUtil.of(res, totalCount, page, size)))
    }

    /**
     * 회원 목록 조회 (커서 기반)
     * - nextCursor: 마지막 row의 id
     */
    @GetMapping("/users/cursor")
    suspend fun getUsersByCursor(
        @RequestParam size: Int = 10,
        @RequestParam(required = false) lastId: Long?,
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam status: String? = null,
        @RequestParam role: String? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPageCursor<AdminUserResponse>>> {
        val list = adminUserRepository.listByCursorId(
            size = size,
            lastId = lastId,
            search = search,
            searchType = searchType,
            role = role,
            status = status,
        ).toList()

        val nextCursor = list.lastOrNull()?.id?.toString()

        return ResponseEntity.ok(
            ApiResult.success(
                ApiPageCursor(
                    items = list,
                    nextCursor = nextCursor,
                    perPage = size,
                )
            )
        )
    }


    @GetMapping("/user")
    suspend fun getUserDetail(
        @RequestParam("uid") uid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<AdminUserResponse>> {

        val data = adminUserRepository.findResponseByUid(uid)
            ?: throw IllegalArgumentException("존재하지 않는 사용자입니다.")

        val userResponse = userService.findUserResponse(data.id)

        val finalResponse = AdminUserResponse.from(data, userResponse)

        return ResponseEntity.ok(ApiResult.success(finalResponse))
    }


    /**
     * 관리자 사용자 등록/수정
     * - request.id == null: 생성
     * - request.id != null: 수정
     * - interestIds/photoUids 가 null이면 연관 데이터는 변경하지 않음
     * - interestIds/photoUids 가 []이면 전체 삭제
     */
    @PostMapping("/save")
    suspend fun saveUser(
        @RequestBody request: AdminUserSaveRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<AdminUserResponse>> {
        val saved = adminUserService.save(request, user)
        return ResponseEntity.ok(ApiResult.success(saved))
    }


    @PostMapping("/password/change")
    suspend fun passwordChange(
        @RequestParam("uid") id: Long,
        @RequestParam("newPassword") newPassword: String,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) {
        val existing = adminUserRepository.findById(id)
            ?: throw UsernameNotFoundException("User not found: ${user.username}")

        val now = OffsetDateTime.now()

        val deleted = existing.copy(
            password = encoder.encode(newPassword),
            updatedAt = now,
            updatedId = user.id
        )
        adminUserRepository.save(deleted)
    }


    @PostMapping("/withdraw")
    suspend fun withdraw(
        @RequestParam id: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) {
        val existing = adminUserRepository.findById(id)
            ?: throw UsernameNotFoundException("User not found: ${id}")

        val now = OffsetDateTime.now()

        val deleted = existing.copy(
            deletedAt = now,
            deletedId = user.id
        )
        adminUserRepository.save(deleted)
    }


    @GetMapping("/friends")
    suspend fun getFriends(
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "DESC",
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam status: String? = null,
        @RequestParam id: Long? = null,
        @RequestParam uid: UUID? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPage<AdminFriendResponse>>> {

        val list = adminFriendRepository.listByPage(
            page = page,
            size = size,
            order = order,
            search = search,
            searchType = searchType,
            status = status,
            uid = uid,
        ).toList()

        val totalCount = adminFriendRepository.totalCount(
            search = search,
            searchType = searchType,
            status = status,
            uid = uid,
        )

        return ResponseEntity.ok(ApiResult.success(PageUtil.of(list, totalCount, page, size)))
    }



    @GetMapping("/follows")
    suspend fun getFollows(
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "DESC",
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam status: String? = null,
        @RequestParam uid: String? = null,

        @RequestParam followType: String = "FOLLOWING",

        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPage<AdminFollowResponse>>> {

        val uuid = uid?.let { UUID.fromString(it) }

        val list = adminFollowRepository.listByPage(
            page = page,
            size = size,
            order = order,
            search = search,
            searchType = searchType,
            status = status,
            uid = uuid,
            followType = followType,
        ).toList()

        val totalCount = adminFollowRepository.totalCount(
            search = search,
            searchType = searchType,
            status = status,
            uid = uuid,
            followType = followType,
        )

        return ResponseEntity.ok(
            ApiResult.success(PageUtil.of(list, totalCount, page, size))
        )
    }

    // 관리자 본인 비밀번호 변경
    @PostMapping("/password/my/change")
    suspend fun changeMyPassword(
        @Valid @RequestBody req: AdminMyPasswordChangeRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Any>> {

        if (!encoder.matches(req.oldPassword, user.password)) {
            return failure("기존 비밀번호가 일치하지 않습니다.")
        }

        val data = user.copy(
            password = encoder.encode(req.newPassword),
            updatedAt = OffsetDateTime.now(),
            updatedId = user.id
        )

        return success(data, "비밀번호가 변경되었습니다.")
    }

    @PostMapping("/role")
    suspend fun updateRole(
        @Parameter @Valid @RequestBody request: AdminUserRoleUpdateRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<AdminUserRoleUpdateResponse>> {
        val data = adminUserService.updateRole(request, user)
        return ResponseEntity.ok(ApiResult.success(data))
    }

    /**
     * 특정 회원의 포인트 조회
     */
    @GetMapping("/users/{userId}/point")
    suspend fun getUserPoint(
        @PathVariable userId: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Int>> {
        return ResponseEntity.ok(ApiResult.success(pointService.getUserTotalPoints(userId)))
    }

    /**
     * 특정 회원의 캐시 조회
     */
    @GetMapping("/users/{userId}/cash")
    suspend fun getUserCash(
        @PathVariable userId: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Int>> {
        return ResponseEntity.ok(ApiResult.success(cashService.getUserTotalCash(userId)))
    }
}
