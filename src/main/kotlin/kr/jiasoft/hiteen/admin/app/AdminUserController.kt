package kr.jiasoft.hiteen.admin.app

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminFollowResponse
import kr.jiasoft.hiteen.admin.dto.AdminFriendResponse
import kr.jiasoft.hiteen.admin.dto.AdminUserResponse
import kr.jiasoft.hiteen.admin.dto.AdminUserSaveRequest
import kr.jiasoft.hiteen.admin.infra.AdminFollowRepository
import kr.jiasoft.hiteen.admin.infra.AdminFriendRepository
import kr.jiasoft.hiteen.admin.infra.AdminUserRepository
import kr.jiasoft.hiteen.admin.services.AdminUserService
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.user.app.UserService
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.lang.IllegalArgumentException
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/admin/user")
class AdminUserController (
    private val userService: UserService,
    private val adminUserRepository: AdminUserRepository,
    private val adminFriendRepository: AdminFriendRepository,
    private val adminFollowRepository: AdminFollowRepository,
    private val encoder: PasswordEncoder,
    private val adminUserService: AdminUserService,
) {


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

        return ResponseEntity.ok(ApiResult.Companion.success(PageUtil.of(res, totalCount, page, size)))
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


}