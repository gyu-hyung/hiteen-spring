package kr.jiasoft.hiteen.feature.banner.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.article.app.ArticleService
import kr.jiasoft.hiteen.feature.article.domain.ArticleStatus
import kr.jiasoft.hiteen.feature.article.dto.ArticleResponse
import kr.jiasoft.hiteen.feature.banner.domain.BannerCategory
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "Banner", description = "배너 관련 API")
@RestController
@RequestMapping("/api/banner")
@SecurityRequirement(name = "bearerAuth")
class BannerController(
    private val service: ArticleService,
) {
    @Operation(summary = "배너 목록 조회")
    @GetMapping
    suspend fun listBanner(
        @Parameter(description = "카테고리(MAIN,AD)") @RequestParam(required = false) category: BannerCategory?,
        @Parameter(description = "상태 (ACTIVE: 진행중, ENDED: 종료, WINNING: 당첨자발표)") @RequestParam(required = false) status: ArticleStatus?,
        @Parameter(description = "검색어") @RequestParam(required = false) q: String?,
        @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "페이지당 개수") @RequestParam(defaultValue = "20") size: Int,
        @AuthenticationPrincipal(expression = "user") user: UserEntity?,
    ): ResponseEntity<ApiResult<ApiPage<ArticleResponse>>> {
        val result = service.listArticlesByPage(category?.name, q, status?.name, page, size)
        return ResponseEntity.ok(ApiResult.success(result))
    }
}

