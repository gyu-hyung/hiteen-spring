package kr.jiasoft.hiteen.feature.board.app

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitSingle
import kr.jiasoft.hiteen.common.dto.ApiPageCursor
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.board.dto.BoardCommentRegisterRequest
import kr.jiasoft.hiteen.feature.board.dto.BoardCommentResponse
import kr.jiasoft.hiteen.feature.board.dto.BoardCreateRequest
import kr.jiasoft.hiteen.feature.board.dto.BoardResponse
import kr.jiasoft.hiteen.feature.board.dto.BoardUpdateRequest
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import java.util.UUID

@RestController
@RequestMapping("/api/boards")
class BoardController(
    private val service: BoardService,
) {

    /** 프로필 조회 */
    @GetMapping("/profile")
    suspend fun userBoards(@AuthenticationPrincipal(expression = "user") user: UserEntity?)
            : ResponseEntity<ApiResult<BoardResponse>> {
        val result = service.getUserBoards(user?.id)
        return ResponseEntity.ok(ApiResult.success(result))
    }


    /** 게시글 목록 */
    @GetMapping
    suspend fun list(
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) q: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @AuthenticationPrincipal(expression = "user") user: UserEntity?
    ): ResponseEntity<ApiResult<List<BoardResponse>>> {
        val boards = service.listBoards(category, q, page, size, user?.id).toList()
        return ResponseEntity.ok(ApiResult.success(boards))
    }


    /** 게시글 단건 조회 */
    @GetMapping("/{uid}")
    suspend fun get(
        @PathVariable uid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity?
    ): ResponseEntity<ApiResult<BoardResponse>> {
        val board = service.getBoard(uid, user?.id)
        return ResponseEntity.ok(ApiResult.success(board))
    }


    /** 게시글 작성 */
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun create(
        req: BoardCreateRequest, // 👈 JSON 파트 지정
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestPart(name = "files", required = false) filesFlux: Flux<FilePart>?,
        request: ServerHttpRequest
    ): ResponseEntity<ApiResult<Map<String, Any>>> {
        val ip = request.remoteAddress?.address?.hostAddress
        val files: List<FilePart> = filesFlux?.collectList()?.awaitSingle().orEmpty()
        val uid = service.createBoard(req, user.id!!, files, ip)
        return ResponseEntity.ok(ApiResult.success(mapOf("uid" to uid)))
    }


    /** 게시글 수정 */
    @PostMapping("/{uid}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun update(
        @RequestPart("data") req: BoardUpdateRequest, // 👈 JSON 파트 지정
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestPart(name = "files", required = false) filesFlux: Flux<FilePart>?,
        request: ServerHttpRequest
    ): ResponseEntity<ApiResult<Unit>> {
        val ip = request.remoteAddress?.address?.hostAddress
        val files: List<FilePart> = filesFlux?.collectList()?.awaitSingle().orEmpty()
        service.updateBoard(req.uid!!, req, user.id!!, files, ip, req.replaceAssets, req.deleteAssetUids)
        return ResponseEntity.ok(ApiResult.success(Unit))
    }


    /** 게시글 삭제 */
    @DeleteMapping("/{uid}")
    suspend fun delete(
        @PathVariable uid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Unit>> {
        service.softDeleteBoard(uid, currentUserId = user.id!!)
        return ResponseEntity.ok(ApiResult.success(Unit))
    }


    /** 좋아요 */
    @PostMapping("/{uid}/like")
    suspend fun like(
        @PathVariable uid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Unit>> {
        service.likeBoard(uid, currentUserId = user.id!!)
        return ResponseEntity.ok(ApiResult.success(Unit))
    }


    /** 좋아요 취소 */
    @DeleteMapping("/{uid}/like")
    suspend fun unlike(
        @PathVariable uid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Unit>> {
        service.unlikeBoard(uid, currentUserId = user.id!!)
        return ResponseEntity.ok(ApiResult.success(Unit))
    }


    /**
     * 댓글 목록 (parentUid 없으면 최상위, 있으면 대댓글)
     * - cursor: 마지막 댓글 uid (커서 기반 페이지네이션)
     * - perPage: 페이지당 개수
     */
    @GetMapping("/comments")
    suspend fun comments(
        @RequestParam boardUid: UUID,
        @RequestParam(required = false) parentUid: UUID?,
        @RequestParam(required = false) cursor: UUID?,
        @RequestParam(defaultValue = "20") perPage: Int,
        @AuthenticationPrincipal(expression = "user") user: UserEntity?,
    ): ResponseEntity<ApiResult<ApiPageCursor<BoardCommentResponse>>> {

        // +1 가져와서 nextCursor 여부 판단
        val list = service.listComments(boardUid, parentUid, user?.id, cursor, perPage + 1)

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
        req: BoardCommentRegisterRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Map<String, Any>>> {
        val uid = service.createComment(req.boardUid!!, req, user.id!!)
        return ResponseEntity.ok(ApiResult.success(mapOf("uid" to uid)))
    }


    /** 댓글 수정 */
    @PostMapping("/comments/update")
    suspend fun updateComment(
        req: BoardCommentRegisterRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Map<String, Any>>> {
        val uid = service.updateComment(req.boardUid!!, req.commentUid!!, req, user.id!!)
        return ResponseEntity.ok(ApiResult.success(mapOf("uid" to uid)))
    }


    /** 댓글 삭제 */
    @DeleteMapping("/comments/delete")
    suspend fun deleteComment(
        req: BoardCommentRegisterRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Map<String, Any>>> {
        val uid = service.deleteComment(req.boardUid!!, req.commentUid!!, user.id!!)
        return ResponseEntity.ok(ApiResult.success(mapOf("uid" to uid)))
    }


    /** 댓글 좋아요 */
    @PostMapping("/comments/like")
    suspend fun likeComment(
        req: BoardCommentRegisterRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Unit>> {
        service.likeComment(req.commentUid!!, user.id!!)
        return ResponseEntity.ok(ApiResult.success(Unit))
    }


    /** 댓글 좋아요 취소 */
    @DeleteMapping("/comments/like")
    suspend fun unlikeComment(
        req: BoardCommentRegisterRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Unit>> {
        service.unlikeComment(req.commentUid!!, user.id!!)
        return ResponseEntity.ok(ApiResult.success(Unit))
    }
}
