package kr.jiasoft.hiteen.admin.app

import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.poll.dto.PollSelectResponse
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/admin/comment")
class AdminCommentController {

    data class CommentAdminResponse (
        val no: Long,
        val id: Long,
        val uid: UUID,
        val question: String,
        val photos: List<String>? = emptyList(),
        val selects: List<PollSelectResponse> = emptyList(),
        val colorStart: String?,
        val colorEnd: String?,
        val voteCount: Int = 0,
        val commentCount: Int = 0,
        val reportCount: Int = 0,
        val likeCount: Int = 0,
        val allowComment: Int,
        val createdAt: OffsetDateTime,
    )

    @GetMapping("/comments/post")
    suspend fun getCommentPosts(
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "DESC",
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam id: Long? = null,
        @RequestParam uid: String? = null,
        @RequestParam status: String? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) : ResponseEntity<ApiResult<ApiPage<CommentAdminResponse>>> {

        val photos = listOf("550e8400-e29b-41d4-a716-446655440000", "550e8400-e29b-41d4-a716-446655440000")
        val selects = listOf(
            PollSelectResponse(1, 1, "김밥", 10, listOf("550e8400-e29b-41d4-a716-446655440000")),
            PollSelectResponse(2, 2, "피자", 10, listOf("550e8400-e29b-41d4-a716-446655440000")),
            PollSelectResponse(3, 3, "탕수육", 10, listOf("550e8400-e29b-41d4-a716-446655440000")),
        )

        val res = listOf(
            CommentAdminResponse(1, 1, UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), "저메추", photos, selects, "#FFFFFF", "#000000", 10, 11, 12, 13, 1, OffsetDateTime.now()),
            CommentAdminResponse(2, 1, UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),"저메추", photos, selects, "#FFFFFF", "#000000", 10, 11, 12, 13, 1, OffsetDateTime.now()),
            CommentAdminResponse(3, 1, UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),"저메추", photos, selects, "#FFFFFF", "#000000", 10, 11, 12, 13, 1, OffsetDateTime.now()),
            CommentAdminResponse(4, 1, UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),"저메추", photos, selects, "#FFFFFF", "#000000", 10, 11, 12, 13, 1, OffsetDateTime.now()),
        )

        //페이징
        val pageData = PageUtil.of(res, res.size, page, size)

        return ResponseEntity.ok(ApiResult.Companion.success(pageData))
    }

    @GetMapping("/comments/vote")
    suspend fun getCommentVote(
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "DESC",
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam id: Long? = null,
        @RequestParam uid: String? = null,
        @RequestParam status: String? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) : ResponseEntity<ApiResult<ApiPage<CommentAdminResponse>>> {

        val photos = listOf("550e8400-e29b-41d4-a716-446655440000", "550e8400-e29b-41d4-a716-446655440000")
        val selects = listOf(
            PollSelectResponse(1, 1, "김밥", 10, listOf("550e8400-e29b-41d4-a716-446655440000")),
            PollSelectResponse(2, 2, "피자", 10, listOf("550e8400-e29b-41d4-a716-446655440000")),
            PollSelectResponse(3, 3, "탕수육", 10, listOf("550e8400-e29b-41d4-a716-446655440000")),
        )

        val res = listOf(
            CommentAdminResponse(1, 1, UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), "저메추", photos, selects, "#FFFFFF", "#000000", 10, 11, 12, 13, 1, OffsetDateTime.now()),
            CommentAdminResponse(2, 1, UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),"저메추", photos, selects, "#FFFFFF", "#000000", 10, 11, 12, 13, 1, OffsetDateTime.now()),
            CommentAdminResponse(3, 1, UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),"저메추", photos, selects, "#FFFFFF", "#000000", 10, 11, 12, 13, 1, OffsetDateTime.now()),
            CommentAdminResponse(4, 1, UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),"저메추", photos, selects, "#FFFFFF", "#000000", 10, 11, 12, 13, 1, OffsetDateTime.now()),
        )

        //페이징
        val pageData = PageUtil.of(
            items = res,
            total = res.size,
            page = page,
            size = size
        )

        return ResponseEntity.ok(ApiResult.Companion.success(pageData))

    }

}