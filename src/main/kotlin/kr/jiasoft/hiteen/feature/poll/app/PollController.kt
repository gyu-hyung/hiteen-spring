package kr.jiasoft.hiteen.feature.poll.app

import kr.jiasoft.hiteen.common.dto.ApiPageCursor
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.poll.dto.*
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*
import kotlin.collections.dropLast
import kotlin.collections.lastOrNull

@RestController
@RequestMapping("/api/polls")
class PollController(
    private val service: PollService
) {

    /** 투표 목록 */
    @GetMapping
    suspend fun list(
        @RequestParam(required = false) cursor: Long?,
        @RequestParam(defaultValue = "20") size: Int,
        @AuthenticationPrincipal(expression = "user") user: UserEntity?
    ): ResponseEntity<ApiResult<ApiPageCursor<PollResponse>>> {
        // +1 개 가져와서 nextCursor 판단
        val list = service.listPollsByCursor(cursor, size + 1, user?.id)

        val hasMore = list.size > size
        val items = if (hasMore) list.dropLast(1) else list
        val nextCursor = if (hasMore) items.lastOrNull()?.id?.toString() else null

        val result = ApiPageCursor(
            nextCursor = nextCursor,
            items = items,
            perPage = size
        )
        return ResponseEntity.ok(ApiResult.success(result))
    }


    /** 투표 상세 */
    @GetMapping("/{id}")
    suspend fun get(
        @PathVariable id: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity?
    ): ResponseEntity<ApiResult<PollResponse>> {
        val poll = service.getPoll(id, user?.id)
        return ResponseEntity.ok(ApiResult.success(poll))
    }


    /** 투표 생성 */
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun create(
        req: PollCreateRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestPart("file", required = false) file: FilePart?,
        ): ResponseEntity<ApiResult<Long>> {
        val id = service.create(req, user.id!!, file)
        return ResponseEntity.ok(ApiResult.success(id))
    }


    /** 투표 수정 */
    @PutMapping("/{id}")
    suspend fun update(
        @PathVariable id: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @ModelAttribute req: PollUpdateRequest,
        @RequestPart(required = false) file: FilePart?,
    ): ResponseEntity<ApiResult<Long>> {
        val updatedId = service.update(id, req, user.id!!, file)
        return ResponseEntity.ok(ApiResult.success(updatedId))
    }


    /** 투표 삭제 */
    @DeleteMapping("/{id}")
    suspend fun delete(
        @PathVariable id: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Unit>> {
        service.softDelete(id, currentUserId = user.id!!)
        return ResponseEntity.ok(ApiResult.success(Unit))
    }


    /** 투표 참여 */
    @PostMapping("/{id}/vote")
    suspend fun vote(
        @PathVariable id: Long,
        req: PollVoteRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<ApiResult<Unit>> {
        service.vote(id, req.seq, user.id!!)
        return ResponseEntity.ok(ApiResult.success(Unit))
    }


    /** 투표 좋아요 */
    @PostMapping("/{id}/like")
    suspend fun like(
        @PathVariable id: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Unit>> {
        service.like(id, currentUserId = user.id!!)
        return ResponseEntity.ok(ApiResult.success(Unit))
    }


    /** 투표 좋아요 취소 */
    @DeleteMapping("/{id}/like")
    suspend fun unlike(
        @PathVariable id: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Unit>> {
        service.unlike(id, currentUserId = user.id!!)
        return ResponseEntity.ok(ApiResult.success(Unit))
    }


    /** 투표 댓글 목록 */
    @GetMapping("/comments")
    suspend fun comments(
        @RequestParam pollId: Long,
        @RequestParam(required = false) parentUid: UUID?,
        @RequestParam(required = false) cursor: UUID?,
        @RequestParam(defaultValue = "20") perPage: Int,
        @AuthenticationPrincipal(expression = "user") user: UserEntity?
    ): ResponseEntity<ApiResult<ApiPageCursor<PollCommentResponse>>> {
        val list = service.listComments(pollId, parentUid, user?.id, cursor, perPage)

        val hasMore = list.size > perPage
        val items = if (hasMore) list.dropLast(1) else list
        val nextCursor = if (hasMore) list.lastOrNull()?.uid?.toString() else null

        val result = ApiPageCursor(
            nextCursor = nextCursor,
            items = items,
            perPage = perPage
        )

        return ResponseEntity.ok(ApiResult.success(result))
    }


    /** 댓글 작성 */
    @PostMapping("/comments")
    suspend fun createComment(
        req: PollCommentRegisterRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<ApiResult<Long>> {
        val commentId = service.createComment(req, user.id!!)
        return ResponseEntity.ok(ApiResult.success(commentId))
    }


    /** 댓글 수정 TODO : 반환 타입? */
    @PostMapping("/comments/{pollId}/{commentUid}")
    suspend fun updateComment(
        req: PollCommentRegisterRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Map<String, Any>>> {
        val uid = service.updateComment(req.pollId!!, req.commentUid!!, req, user.id!!)
        return ResponseEntity.ok(ApiResult.success(mapOf("uid" to uid)))
    }


    /** 댓글 삭제 */
    @DeleteMapping("/comments/{pollId}/{commentUid}")
    suspend fun deleteComment(
        req: PollCommentRegisterRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Map<String, Any>>> {
        val uid = service.deleteComment(req.pollId!!, req.commentUid!!, user.id!!)
        return ResponseEntity.ok(ApiResult.success(mapOf("uid" to uid)))
    }


    /** 댓글 좋아요 */
    @PostMapping("/comments/like/{commentUid}")
    suspend fun likeComment(
        req: PollCommentRegisterRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Unit>> {
        service.likeComment(req.commentUid!!, user.id!!)
        return ResponseEntity.ok(ApiResult.success(Unit))
    }


    /** 댓글 좋아요 취소 */
    @DeleteMapping("/comments/like/{commentUid}")
    suspend fun unlikeComment(
        req: PollCommentRegisterRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Unit>> {
        service.unlikeComment(req.commentUid!!, user.id!!)
        return ResponseEntity.ok(ApiResult.success(Unit))
    }

}
