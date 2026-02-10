package kr.jiasoft.hiteen.feature.article.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiPageCursor
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.article.domain.ArticleCategory
import kr.jiasoft.hiteen.feature.article.domain.ArticleStatus
import kr.jiasoft.hiteen.feature.article.dto.ArticleDetailResponse
import kr.jiasoft.hiteen.feature.article.dto.ArticleResponse
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "Article", description = "공지사항/이벤트 관련 API")
@RestController
@RequestMapping("/api/articles")
@SecurityRequirement(name = "bearerAuth")
class ArticleController(
    private val service: ArticleService,
) {

    @Operation(
        summary = "공지사항/이벤트 목록 조회 (페이지 기반)",
        description = "카테고리(NOTICE/EVENT), 상태(ACTIVE/ENDED/WINNER_ANNOUNCED), 검색어, 페이지 기반 페이지네이션으로 목록을 조회합니다."
    )
    @GetMapping
    suspend fun listByPage(
        @Parameter(description = "카테고리 (NOTICE/EVENT)") @RequestParam(required = false) category: ArticleCategory?,
        @Parameter(description = "상태 (ACTIVE: 진행중, ENDED: 종료, WINNER_ANNOUNCED: 당첨자발표)") @RequestParam(required = false) status: ArticleStatus?,
        @Parameter(description = "검색어") @RequestParam(required = false) q: String?,
        @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "페이지당 개수") @RequestParam(defaultValue = "20") size: Int,
        @AuthenticationPrincipal(expression = "user") user: UserEntity?,
    ): ResponseEntity<ApiResult<ApiPage<ArticleResponse>>> {
        val result = service.listArticlesByPage(category?.name, q, status?.name, page, size)
        return ResponseEntity.ok(ApiResult.success(result))
    }

    @Operation(
        summary = "공지사항/이벤트 목록 조회 (커서 기반)",
        description = "카테고리(NOTICE/EVENT), 상태(ACTIVE/ENDED/WINNER_ANNOUNCED), 검색어, 커서 기반 페이지네이션으로 목록을 조회합니다."
    )
    @GetMapping("/cursor")
    suspend fun listByCursor(
        @Parameter(description = "카테고리 (NOTICE/EVENT)") @RequestParam(required = false) category: ArticleCategory?,
        @Parameter(description = "상태 (ACTIVE: 진행중, ENDED: 종료, WINNER_ANNOUNCED: 당첨자발표)") @RequestParam(required = false) status: ArticleStatus?,
        @Parameter(description = "검색어") @RequestParam(required = false) q: String?,
        @Parameter(description = "조회 개수 (기본 20)") @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "커서 (마지막 article id)") @RequestParam(required = false) cursor: Long?,
        @AuthenticationPrincipal(expression = "user") user: UserEntity?,
    ): ResponseEntity<ApiResult<ApiPageCursor<ArticleResponse>>> {
        val result = service.listArticlesByCursor(category?.name, q, status?.name, size, cursor)
        return ResponseEntity.ok(ApiResult.success(result))
    }

    @Operation(summary = "공지사항/이벤트 단건 조회", description = "특정 공지사항/이벤트를 ID로 조회합니다. 이전글/다음글 정보를 포함합니다.")
    @GetMapping("/{id}")
    suspend fun get(
        @Parameter(description = "게시글 ID") @PathVariable id: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity?,
    ): ResponseEntity<ApiResult<ArticleDetailResponse>> {
        val article = service.getArticle(id, user?.id)
        return ResponseEntity.ok(ApiResult.success(article))
    }
}

