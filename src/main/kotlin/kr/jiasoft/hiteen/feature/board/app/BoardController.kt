package kr.jiasoft.hiteen.feature.board.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.reactive.awaitSingle
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiPageCursor
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.board.domain.BoardCategory
import kr.jiasoft.hiteen.feature.board.dto.*
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import java.util.*

@Tag(name = "Board", description = "게시판 관련 API")
@RestController
@RequestMapping("/api/boards")
@SecurityRequirement(name = "bearerAuth")   // 🔑 JWT 인증 필요
class BoardController(
    private val service: BoardService,
) {

    @Operation(
        summary = "게시글 목록 조회",
        description = "카테고리, 검색어, 작성자, 커서 기반 페이지네이션 옵션을 이용해 게시글 목록을 조회합니다."
    )
    @GetMapping
    suspend fun list(
        @Parameter(description = "카테고리") @RequestParam(required = false) category: BoardCategory = BoardCategory.POST,
        @Parameter(description = "검색어") @RequestParam(required = false) q: String?,
        @Parameter(description = "조회 개수 (기본 20)") @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "커서 UUID") @RequestParam(required = false) cursor: UUID?,
        @Parameter(description = "작성자 UUID") @RequestParam(required = false) author: UUID?,
        @Parameter(description = "팔로우한 사용자만") @RequestParam(defaultValue = "false") followOnly: Boolean,
        @Parameter(description = "친구만") @RequestParam(defaultValue = "false") friendOnly: Boolean,
        @Parameter(description = "같은 학교만") @RequestParam(defaultValue = "false") sameSchoolOnly: Boolean,
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<ApiResult<ApiPageCursor<BoardResponse>>> {
        val boards = service.listBoardsByCursor(
            category, q, size, user.id,
            followOnly, friendOnly, sameSchoolOnly, cursor, author
        )
        return ResponseEntity.ok(ApiResult.success(boards))
    }

    @Operation(summary = "게시글 단건 조회", description = "특정 게시글을 UID로 조회합니다.")
    @GetMapping("/{boardUid}")
    suspend fun get(
        @Parameter(description = "게시글 UID") @PathVariable boardUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<ApiResult<BoardResponse>> {
        val board = service.getBoard(boardUid, user.id)
        return ResponseEntity.ok(ApiResult.success(board))
    }

    @Operation(summary = "게시글 작성", description = "게시글을 작성하고 파일(이미지 등)을 업로드할 수 있습니다.")
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun create(
        @Parameter(description = "게시글 생성 요청 DTO") boardCreateRequest: BoardCreateRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "첨부 파일들") @RequestPart(name = "files", required = false) filesFlux: Flux<FilePart>?,
        request: ServerHttpRequest
    ): ResponseEntity<ApiResult<Map<String, Any>>> {
        println("✅✅ ✅✅ ✅✅ ✅✅ ✅✅ ✅✅ ✅✅ ✅✅ ✅✅ ✅✅ ")
        val ip = request.remoteAddress?.address?.hostAddress
        val files: List<FilePart> = filesFlux?.collectList()?.awaitSingle().orEmpty()
        val uid = service.create(boardCreateRequest, user, files, ip)
        return ResponseEntity.ok(ApiResult.success(mapOf("uid" to uid)))
    }

    @Operation(summary = "게시글 수정", description = "기존 게시글 내용을 수정합니다.")
    @PostMapping("/{boardUid}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun update(
        @Parameter(description = "게시글 수정 요청 DTO") boardUpdateRequest: BoardUpdateRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "첨부 파일들") @RequestPart(name = "files", required = false) filesFlux: Flux<FilePart>?,
        request: ServerHttpRequest
    ): ResponseEntity<ApiResult<Unit>> {
        val ip = request.remoteAddress?.address?.hostAddress
        val files: List<FilePart> = filesFlux?.collectList()?.awaitSingle().orEmpty()
        service.update(boardUpdateRequest.boardUid, boardUpdateRequest, user.id, files, ip, boardUpdateRequest.replaceAssets, boardUpdateRequest.deleteAssetUids)
        return ResponseEntity.ok(ApiResult.success(Unit))
    }

    @Operation(summary = "게시글 삭제", description = "특정 게시글을 삭제합니다.")
    @DeleteMapping("/{boardUid}")
    suspend fun delete(
        @Parameter(description = "게시글 UID") @PathVariable boardUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Unit>> {
        service.softDelete(boardUid, currentUserId = user.id)
        return ResponseEntity.ok(ApiResult.success(Unit))
    }

    @Operation(summary = "게시글 좋아요", description = "게시글에 좋아요를 추가합니다.")
    @PostMapping("/like/{boardUid}")
    suspend fun like(
        @Parameter(description = "게시글 UID") @PathVariable boardUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Unit>> {
        service.like(boardUid, currentUserId = user.id)
        return ResponseEntity.ok(ApiResult.success(Unit))
    }

    @Operation(summary = "게시글 좋아요 취소", description = "게시글 좋아요를 취소합니다.")
    @DeleteMapping("/like/{boardUid}")
    suspend fun unlike(
        @Parameter(description = "게시글 UID") @PathVariable boardUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Unit>> {
        service.unlike(boardUid, currentUserId = user.id)
        return ResponseEntity.ok(ApiResult.success(Unit))
    }

    @Operation(
        summary = "댓글 목록 조회",
        description = "특정 게시글의 댓글 목록을 커서 기반 페이지네이션으로 조회합니다. parentUid가 없으면 최상위 댓글만 조회합니다."
    )
    @GetMapping("/comments/{boardUid}")
    suspend fun comments(
        @Parameter(description = "게시글 UID") @PathVariable boardUid: UUID,
        @Parameter(description = "부모 댓글 UID") @RequestParam(required = false) parentUid: UUID?,
        @Parameter(description = "커서 UUID") @RequestParam(required = false) cursor: UUID?,
        @Parameter(description = "페이지당 댓글 개수 (기본 20)") @RequestParam(defaultValue = "20") perPage: Int,
        @AuthenticationPrincipal(expression = "user") user: UserEntity?,
    ): ResponseEntity<ApiResult<ApiPageCursor<BoardCommentResponse>>> {
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

    @Operation(
        summary = "내 댓글 목록 조회",
        description = "로그인한 사용자가 작성한 댓글 목록을 커서 기반 페이지네이션으로 조회합니다."
    )
    @GetMapping("/comments/me")
    suspend fun myComments(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "커서 UUID") @RequestParam(required = false) cursor: UUID?,
        @Parameter(description = "페이지당 댓글 개수 (기본 20)") @RequestParam(defaultValue = "20") perPage: Int,
    ): ResponseEntity<ApiResult<ApiPageCursor<BoardCommentResponse>>> {
        val list = service.listMyComments(user.id, cursor, perPage + 1)

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


    @Operation(summary = "댓글 작성", description = "특정 게시글에 댓글을 작성합니다.")
    @PostMapping("/comments/{boardUid}")
    suspend fun createComment(
        @Parameter(description = "댓글 등록/수정 요청 DTO") boardCommentRegisterRequest: BoardCommentRegisterRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<BoardCommentResponse>> {
        val result = service.createComment(boardCommentRegisterRequest.boardUid, boardCommentRegisterRequest, user)
        return ResponseEntity.ok(ApiResult.success(result))
    }

    @Operation(summary = "댓글 수정", description = "특정 댓글을 수정합니다.")
    @PostMapping("/comments/{boardUid}/{commentUid}")
    suspend fun updateComment(
        @Parameter(description = "댓글 등록/수정 요청 DTO") boardCommentRegisterRequest: BoardCommentRegisterRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Map<String, Any>>> {
        val uid = service.updateComment(boardCommentRegisterRequest.boardUid, boardCommentRegisterRequest.commentUid!!, boardCommentRegisterRequest, user.id)
        return ResponseEntity.ok(ApiResult.success(mapOf("uid" to uid)))
    }

    @Operation(summary = "댓글 삭제", description = "특정 댓글을 삭제합니다.")
    @DeleteMapping("/comments/{boardUid}/{commentUid}")
    suspend fun deleteComment(
        @Parameter(description = "게시글 UUID") @PathVariable boardUid: UUID,
        @Parameter(description = "댓글 UUID") @PathVariable commentUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Map<String, Any>>> {
        val uid = service.deleteComment(boardUid, commentUid, user.id)
        return ResponseEntity.ok(ApiResult.success(mapOf("uid" to uid)))
    }

    @Operation(summary = "댓글 좋아요", description = "특정 댓글에 좋아요를 추가합니다.")
    @PostMapping("/comments/like/{commentUid}")
    suspend fun likeComment(
        @Parameter(description = "댓글 UUID") @PathVariable commentUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Unit>> {
        service.likeComment(commentUid, user.id)
        return ResponseEntity.ok(ApiResult.success(Unit))
    }

    @Operation(summary = "댓글 좋아요 취소", description = "특정 댓글 좋아요를 취소합니다.")
    @DeleteMapping("/comments/like/{commentUid}")
    suspend fun unlikeComment(
        @Parameter(description = "댓글 UUID") @PathVariable commentUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Unit>> {
        service.unlikeComment(commentUid, user.id)
        return ResponseEntity.ok(ApiResult.success(Unit))
    }
}
