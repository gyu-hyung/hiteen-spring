package kr.jiasoft.hiteen.admin.app

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminFollowResponse
import kr.jiasoft.hiteen.admin.dto.AdminFriendResponse
import kr.jiasoft.hiteen.admin.dto.AdminUserResponse
import kr.jiasoft.hiteen.admin.infra.AdminFollowRepository
import kr.jiasoft.hiteen.admin.infra.AdminFriendRepository
import kr.jiasoft.hiteen.admin.infra.AdminUserRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.user.app.UserService
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.lang.IllegalArgumentException
import java.util.UUID

@RestController
@RequestMapping("/api/admin/user")
class AdminUserController (
    private val userService: UserService,
    private val adminUserRepository: AdminUserRepository,
    private val adminFriendRepository: AdminFriendRepository,
    private val adminFollowRepository: AdminFollowRepository,
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

        val data = adminUserRepository.findByUid(uid)
            ?: throw IllegalArgumentException("존재하지 않는 사용자입니다.")

        val userResponse = userService.findUserResponse(data.id)

        val finalResponse = AdminUserResponse.from(data, userResponse)

        return ResponseEntity.ok(ApiResult.success(finalResponse))
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

        // 1) 목록 조회
        val list = adminFriendRepository.listByPage(
            page = page,
            size = size,
            order = order,
            search = search,
            searchType = searchType,
            status = status,
            uid = uid,
        ).toList()

        // 2) 전체 개수 조회
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