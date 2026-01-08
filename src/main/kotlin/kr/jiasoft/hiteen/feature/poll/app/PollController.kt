package kr.jiasoft.hiteen.feature.poll.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.jiasoft.hiteen.common.dto.ApiPageCursor
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.poll.dto.*
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import java.util.*

@Tag(name = "Poll", description = "투표 관련 API")
@SecurityRequirement(name = "bearerAuth")   // 🔑 Bearer 인증 요구
@RestController
@RequestMapping("/api/polls")
class PollController(
    private val service: PollService
) {

    @Operation(
        summary = "투표 목록",
        description = "커서 기반 페이지네이션 방식으로 투표 목록을 조회합니다. type 파라미터로 mine(내가 등록한), participated(내가 참여한) 필터링 가능."
    )
    @GetMapping
    suspend fun list(
        @Parameter(description = "이전 페이지의 마지막 ID") @RequestParam(required = false) cursor: Long?,
        @Parameter(description = "가져올 개수 (기본값 20)") @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "목록 타입: all, mine, participated") @RequestParam(defaultValue = "all") type: String,
        @Parameter(description = "작성자 UUID") @RequestParam(required = false) author: UUID?,
        @AuthenticationPrincipal(expression = "user") user: UserEntity?
    ): ResponseEntity<ApiResult<ApiPageCursor<PollResponse>>> {
        val list = service.listPollsByCursor(cursor, size + 1, user?.id, type, author)

        val hasMore = list.size > size
        val items = if (hasMore) list.dropLast(1) else list
        val nextCursor = if (hasMore) items.lastOrNull()?.id?.toString() else null

        return ResponseEntity.ok(
            ApiResult.success(
                ApiPageCursor(nextCursor, items, size)
            )
        )
    }


    @Operation(summary = "투표 상세", description = "투표 ID로 투표 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    suspend fun get(
        @Parameter(description = "투표 ID") @PathVariable id: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<ApiResult<PollResponse>> {
        val poll = service.getPoll(id, user.id)
        return ResponseEntity.ok(ApiResult.success(poll))
    }

    @Operation(summary = "투표 생성", description = """
        새로운 투표를 생성합니다.
        `files` 로 투표 본문의 이미지와, 선택항목의 이미지를 받습니다.
        파일명으로 아래와 같은 규칙의 파일만 인식합니다. (숫자는 1부터 시작, 이미지 없는 선택항목도 있음)
        ex)
        본문 : poll_1, poll_2, poll_3
        선택항목 : select_1_1, select_1_2, select_2_1, select_4_1
    """)
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun create(
        @Parameter(description = "투표 생성 요청 DTO") pollCreateRequest: PollCreateRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "첨부 파일") @RequestPart("files", required = false) files: Flux<FilePart>?,
    ): ResponseEntity<ApiResult<Long>> {
        val id = service.create(pollCreateRequest, user, files)
        return ResponseEntity.ok(ApiResult.success(id))
    }


    @Operation(summary = "투표 수정 메타정보만", description = """
        투표 메타데이터(제목, 색상, 댓글 허용 여부 등)만 간단히 수정하는 함수
        이미지, 선택지, 파일 변경 없음
    """)
    @PatchMapping("/{id}/meta")
    suspend fun updatePollMeta(
        @PathVariable id: Long,
        @RequestBody req: PollUpdateRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<ApiResult<Long>> {
        val updatedId = service.updateMeta(id, req, user.id)
        return ResponseEntity.ok(ApiResult.success(updatedId))
    }


    @Operation(summary = "투표 수정", description = """
        기존 투표를 수정합니다. 선택항목, 이미지, 메타데이터 등
        이미지, 선택지, 파일 변경 있음
    """)
    @PostMapping("/{id}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun update(
        @Parameter(description = "투표 수정 요청 DTO") @ModelAttribute pollUpdateRequest: PollUpdateRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "첨부 파일") @RequestPart(required = false) files: Flux<FilePart>?,
    ): ResponseEntity<ApiResult<Long>> {
        val updatedId = service.update(pollUpdateRequest.id, pollUpdateRequest, user.id, files)
        return ResponseEntity.ok(ApiResult.success(updatedId))
    }

    @Operation(summary = "투표 삭제", description = "특정 투표를 삭제합니다.")
    @DeleteMapping("/{id}")
    suspend fun delete(
        @Parameter(description = "삭제할 투표 ID") @PathVariable id: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Unit>> {
        service.softDelete(id, currentUserId = user.id)
        return ResponseEntity.ok(ApiResult.success(Unit))
    }

    @Operation(summary = "투표 참여", description = "투표 항목에 참여합니다.")
    @PostMapping("/vote/{pollId}")
    suspend fun vote(
        @Parameter(description = "투표 요청 DTO") pollVoteRequest: PollVoteRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<ApiResult<Unit>> {
        service.vote(pollVoteRequest.pollId, pollVoteRequest.seq, user.id)
        return ResponseEntity.ok(ApiResult.success(Unit))
    }

    @Operation(summary = "투표 좋아요", description = "투표에 좋아요를 추가합니다.")
    @PostMapping("/like/{id}")
    suspend fun like(
        @PathVariable id: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Unit>> {
        service.like(id, currentUserId = user.id)
        return ResponseEntity.ok(ApiResult.success(Unit))
    }

    @Operation(summary = "투표 좋아요 취소", description = "투표 좋아요를 취소합니다.")
    @DeleteMapping("/like/{id}")
    suspend fun unlike(
        @PathVariable id: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Unit>> {
        service.unlike(id, currentUserId = user.id)
        return ResponseEntity.ok(ApiResult.success(Unit))
    }

    @Operation(summary = "투표 댓글 목록", description = "특정 투표의 댓글 목록을 조회합니다. 커서 기반 페이지네이션 지원.")
    @GetMapping("/comments/{pollId}")
    suspend fun comments(
        @Parameter(description = "투표 ID") @PathVariable pollId: Long,
        @Parameter(description = "부모 댓글 UID") @RequestParam(required = false) parentUid: UUID?,
        @Parameter(description = "커서 UID") @RequestParam(required = false) cursor: UUID?,
        @Parameter(description = "페이지 당 댓글 수 (기본값 20)") @RequestParam(defaultValue = "20") perPage: Int,
        @AuthenticationPrincipal(expression = "user") user: UserEntity?
    ): ResponseEntity<ApiResult<ApiPageCursor<PollCommentResponse>>> {
        val list = service.listComments(pollId, parentUid, user?.id, cursor, perPage + 1)

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

    @Operation(summary = "댓글 작성", description = "투표에 댓글을 작성합니다.")
    @PostMapping("/comments/{pollId}")
    suspend fun createComment(
        @Parameter(description = "댓글 등록/수정 요청 DTO") pollCommentRegisterRequest: PollCommentRegisterRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<ApiResult<PollCommentResponse>> {
        val commentId = service.createComment(pollCommentRegisterRequest, user)
        return ResponseEntity.ok(ApiResult.success(commentId))
    }

    //TODO : 반환 타입?
    @Operation(summary = "댓글 수정", description = "기존 댓글을 수정합니다. commentUid 필수")
    @PostMapping("/comments/{pollId}/{commentUid}")
    suspend fun updateComment(
        @Parameter(description = "댓글 등록/수정 요청 DTO") pollCommentRegisterRequest: PollCommentRegisterRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Map<String, Any>>> {
        val uid = service.updateComment(pollCommentRegisterRequest.pollId, pollCommentRegisterRequest.commentUid!!, pollCommentRegisterRequest, user.id)
        return ResponseEntity.ok(ApiResult.success(mapOf("uid" to uid)))
    }

    @Operation(summary = "댓글 삭제", description = "특정 댓글을 삭제합니다.")
    @DeleteMapping("/comments/{pollId}/{commentUid}")
    suspend fun deleteComment(
        @Parameter(description = "투표 ID") @PathVariable pollId: Long,
        @Parameter(description = "댓글 UUID") @PathVariable commentUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Map<String, Any>>> {
        val uid = service.deleteComment(pollId, commentUid, user.id)
        return ResponseEntity.ok(ApiResult.success(mapOf("uid" to uid)))
    }

    @Operation(summary = "댓글 좋아요", description = "댓글에 좋아요를 추가합니다.")
    @PostMapping("/comments/like/{commentUid}")
    suspend fun likeComment(
        @Parameter(description = "댓글 UUID") @PathVariable commentUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Unit>> {
        service.likeComment(commentUid, user.id)
        return ResponseEntity.ok(ApiResult.success(Unit))
    }

    @Operation(summary = "댓글 좋아요 취소", description = "댓글 좋아요를 취소합니다.")
    @DeleteMapping("/comments/like/{commentUid}")
    suspend fun unlikeComment(
        @Parameter(description = "댓글 UUID") @PathVariable commentUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Unit>> {
        service.unlikeComment(commentUid, user.id)
        return ResponseEntity.ok(ApiResult.success(Unit))
    }
}
