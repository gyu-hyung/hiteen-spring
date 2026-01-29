package kr.jiasoft.hiteen.admin.app

import kr.jiasoft.hiteen.admin.dto.AdminAcceptFollowResponse
import kr.jiasoft.hiteen.admin.dto.AdminAcceptFriendResponse
import kr.jiasoft.hiteen.admin.services.AdminAcceptService
import kr.jiasoft.hiteen.common.extensions.success
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/admin/accept")
class AdminAcceptController(
    private val acceptService: AdminAcceptService,
) {
    // 친구 요청/승인 목록
    @GetMapping("/friends")
    suspend fun getFriends(
        @RequestParam status: String? = null,
        @RequestParam type: String? = null,
        @RequestParam startDate: LocalDate? = null,
        @RequestParam endDate: LocalDate? = null,
        @RequestParam searchType: String? = null,
        @RequestParam search: String? = null,
        @RequestParam sort: String = "DESC",
        @RequestParam page: Int = 1,
        @RequestParam perPage: Int = 10,
    ): ResponseEntity<ApiResult<ApiPage<AdminAcceptFriendResponse>>> {
        val startDate = startDate?.atStartOfDay()
        val endDate = endDate?.plusDays(1)?.atStartOfDay()

        val data = acceptService.friendList(
            status, type, startDate, endDate, searchType, search, sort, page, perPage
        )

        return success(data)
    }

    // 팔로우 요청/승인 목록
    @GetMapping("/follows")
    suspend fun geFollows(
        @RequestParam status: String? = null,
        @RequestParam startDate: LocalDate? = null,
        @RequestParam endDate: LocalDate? = null,
        @RequestParam searchType: String? = null,
        @RequestParam search: String? = null,
        @RequestParam sort: String = "DESC",
        @RequestParam page: Int = 1,
        @RequestParam perPage: Int = 10,
    ): ResponseEntity<ApiResult<ApiPage<AdminAcceptFollowResponse>>> {
        val startDate = startDate?.atStartOfDay()
        val endDate = endDate?.plusDays(1)?.atStartOfDay()

        val data = acceptService.followList(
            status, startDate, endDate, searchType, search, sort, page, perPage
        )

        return success(data)
    }
}
