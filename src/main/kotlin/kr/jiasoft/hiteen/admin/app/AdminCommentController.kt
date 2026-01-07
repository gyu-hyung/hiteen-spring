package kr.jiasoft.hiteen.admin.app

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminCommentResponse
import kr.jiasoft.hiteen.admin.infra.AdminBoardCommentRepository
import kr.jiasoft.hiteen.admin.infra.AdminPollCommentRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/admin/comment")
class AdminCommentController (
    private val adminPollCommentRepository: AdminPollCommentRepository,
    private val adminBoardCommentRepository: AdminBoardCommentRepository,
) {


    /**
     * 게시글 댓글 목록 조회
     */
    @GetMapping
    suspend fun list(
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "DESC",
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam status: String? = null,

        @RequestParam uid: UUID? = null,
        @RequestParam type: String = "BOARD",

        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPage<AdminCommentResponse>>> {

        var list = emptyList<AdminCommentResponse>()
        var totalCount = 0

        if (type.equals("BOARD")) {
            list = adminBoardCommentRepository.listByPage(
                page = page,
                size = size,
                order = order,
                search = search,
                searchType = searchType,
                status = status,
                uid = uid,
            ).toList()

            totalCount = adminBoardCommentRepository.totalCount(
                search = search,
                searchType = searchType,
                status = status,
                uid = uid,
            )
        } else {
            list = adminPollCommentRepository.listByPage(
                page = page,
                size = size,
                order = order,
                search = search,
                searchType = searchType,
                status = status,
                uid = uid,
            ).toList()

            totalCount = adminPollCommentRepository.totalCount(
                search = search,
                searchType = searchType,
                status = status,
                uid = uid,
            )
        }


        return ResponseEntity.ok(
            ApiResult.success(PageUtil.of(list, totalCount, page, size))
        )
    }



}