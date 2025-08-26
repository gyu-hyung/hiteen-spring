package kr.jiasoft.hiteen.feature.board.app

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.awaitSingle
import kr.jiasoft.hiteen.feature.board.dto.BoardCommentCreateRequest
import kr.jiasoft.hiteen.feature.board.dto.BoardCommentResponse
import kr.jiasoft.hiteen.feature.board.dto.BoardCreateRequest
import kr.jiasoft.hiteen.feature.board.dto.BoardDetailResponse
import kr.jiasoft.hiteen.feature.board.dto.BoardSummaryResponse
import kr.jiasoft.hiteen.feature.board.dto.BoardUpdateRequest
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.HttpStatus
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import java.util.UUID

@RestController
@RequestMapping("/api/boards")
class BoardController(
    private val service: BoardService,
) {

    /** 게시글 목록 */
    @GetMapping
    fun list(
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) q: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @AuthenticationPrincipal(expression = "user") user: UserEntity?
    ): Flow<BoardSummaryResponse> =
        service.listBoards(category, q, page, size, user?.id)


    /** 게시글 단건 조회 */
    @GetMapping("/{uid}")
    suspend fun get(
        @PathVariable uid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity?
    ): BoardDetailResponse = service.getBoard(uid, user?.id)


    /** 게시글 작성 */
    @PostMapping
    suspend fun create(
        req: BoardCreateRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestPart(name = "files", required = false) filesFlux: Flux<FilePart>?,
        request: ServerHttpRequest
    ): Map<String, Any> {
        val ip = request.remoteAddress?.address?.hostAddress

        val flux = filesFlux ?: filesFlux
        ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "files or file part is required")

        val files: List<FilePart> = flux.collectList().awaitSingle()

        val uid = service.createBoard(req, user.id!!, files, ip)
        return mapOf("uid" to uid)
    }


    /** 게시글 수정 */
    @PostMapping("/{uid}")
    suspend fun update(
        @PathVariable uid: UUID,
        req: BoardUpdateRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestPart(name = "files", required = false) filesFlux: Flux<FilePart>?,
        request: ServerHttpRequest
    ) {
        val ip = request.remoteAddress?.address?.hostAddress
        val flux = filesFlux ?: filesFlux
        ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "files or file part is required")

        val files: List<FilePart> = flux.collectList().awaitSingle()

        service.updateBoard(uid, req, user.id!!, files, ip, req.replaceAssets, req.deleteAssetUids)
    }


    /** 게시글 삭제(소프트) */
    @DeleteMapping("/{uid}")
    suspend fun delete(
        @PathVariable uid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) {
        service.softDeleteBoard(uid, currentUserId = user.id!!)
    }


    /** 좋아요 */
    @PostMapping("/{uid}/like")
    suspend fun like(
        @PathVariable uid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) { service.likeBoard(uid, currentUserId = user.id!!) }


    /** 좋아요 취소 */
    @DeleteMapping("/{uid}/like")
    suspend fun unlike(
        @PathVariable uid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) { service.unlikeBoard(uid, currentUserId = user.id!!) }


    /** 최상위 댓글 목록 */
    @GetMapping("/{uid}/comments")
    fun comments(
        @PathVariable uid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity?,
    ): Flow<BoardCommentResponse> = service.listTopComments(uid, user?.id)


    /** 대댓글 목록 */
    @GetMapping("/comments/{parentUid}/replies")
    fun replies(
        @PathVariable parentUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity?,
    ): Flow<BoardCommentResponse> = service.listReplies(parentUid, user?.id)


    /** 댓글 작성 (parentUid 있으면 대댓글) */
    @PostMapping("/{uid}/comments")
    suspend fun createComment(
        @PathVariable uid: UUID,
        @RequestBody req: BoardCommentCreateRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): Map<String, Any> = mapOf("uid" to service.createComment(uid, req, user.id!!))


    /** 댓글 좋아요 / 취소 */
    @PostMapping("/comments/{commentUid}/like")
    suspend fun likeComment(
        @PathVariable commentUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) { service.likeComment(commentUid, user.id!!) }


    @DeleteMapping("/comments/{commentUid}/like")
    suspend fun unlikeComment(
        @PathVariable commentUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) { service.unlikeComment(commentUid, user.id!!) }
}