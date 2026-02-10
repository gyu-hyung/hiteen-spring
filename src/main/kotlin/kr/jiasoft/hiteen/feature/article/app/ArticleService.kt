package kr.jiasoft.hiteen.feature.article.app

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiPageCursor
import kr.jiasoft.hiteen.feature.article.domain.ArticleAssetType
import kr.jiasoft.hiteen.feature.article.domain.ArticleCategory
import kr.jiasoft.hiteen.feature.article.dto.ArticleDetailResponse
import kr.jiasoft.hiteen.feature.article.dto.ArticleNavigation
import kr.jiasoft.hiteen.feature.article.dto.ArticleResponse
import kr.jiasoft.hiteen.feature.article.infra.ArticleAssetRepository
import kr.jiasoft.hiteen.feature.article.infra.ArticleRepository
import kr.jiasoft.hiteen.feature.level.app.ExpService
import org.springframework.stereotype.Service

@Service
class ArticleService(
    private val articles: ArticleRepository,
    private val articleAssetRepository: ArticleAssetRepository,
    private val expService: ExpService,
) {

    private fun isEvent(category: String) = category == ArticleCategory.EVENT.name

    suspend fun getArticle(id: Long, currentUserId: Long?): ArticleDetailResponse {
        val article = articles.findByIdAndNotDeleted(id)
            ?: throw IllegalArgumentException("해당 글을 찾을 수 없습니다.")

        // 조회수 증가
        articles.increaseHits(id)

        // 공지사항/이벤트 확인 시 경험치 부여
        if (currentUserId != null && (article.category == "NOTICE" || article.category == "EVENT")) {
            expService.grantExp(currentUserId, "NOTICE_READ", article.id)
        }

        // 첨부파일 조회
        val allAssets = articleAssetRepository.findAllByArticleId(article.id).toList()

        // 이전글/다음글 조회
        val prev = articles.findPreviousArticle(id, article.category)
        val next = articles.findNextArticle(id, article.category)

        val prevArticle = prev?.let {
            ArticleNavigation(
                id = prev.id,
                subject = prev.subject,
                createdAt = prev.createdAt
            )
        }
        val nextArticle = next?.let {
            ArticleNavigation(
                id = next.id,
                subject = next.subject,
                createdAt = next.createdAt
            )
        }

        return if (isEvent(article.category)) {
            val large = allAssets.filter { it.assetType == ArticleAssetType.LARGE_BANNER.name }.map { it.uid }
            val small = allAssets.filter { it.assetType == ArticleAssetType.SMALL_BANNER.name }.map { it.uid }
            ArticleDetailResponse(
                id = article.id,
                category = article.category,
                subject = article.subject,
                content = article.content,
                link = article.link,
                hits = article.hits + 1,
                largeBanners = large,
                smallBanners = small,
                startDate = article.startDate,
                endDate = article.endDate,
                status = article.status,
                createdAt = article.createdAt,
                updatedAt = article.updatedAt,
                prevArticle = prevArticle,
                nextArticle = nextArticle,
            )
        } else {
            val attachments = allAssets.filter { it.assetType == ArticleAssetType.ATTACHMENT.name }.map { it.uid }
            ArticleDetailResponse(
                id = article.id,
                category = article.category,
                subject = article.subject,
                content = article.content,
                link = article.link,
                hits = article.hits + 1,
                attachments = attachments,
                startDate = article.startDate,
                endDate = article.endDate,
                status = article.status,
                createdAt = article.createdAt,
                updatedAt = article.updatedAt,
                prevArticle = prevArticle,
                nextArticle = nextArticle,
            )
        }
    }

    suspend fun listArticlesByPage(
        category: String?,
        search: String?,
        status: String?,
        page: Int,
        size: Int,
    ): ApiPage<ArticleResponse> {
        val p = page.coerceAtLeast(0)
        val s = size.coerceIn(1, 100)
        val offset = p * s

        val total = articles.countByCategory(category, search, status)
        val lastPage = if (total == 0) 0 else (total - 1) / s

        val rows = articles.searchByPage(category, search, status, s, offset).toList()

        // 첨부파일 일괄 조회
        val articleIds = rows.map { it.id }
        val allAssets = if (articleIds.isEmpty()) {
            emptyList()
        } else {
            articleAssetRepository.findAllByArticleIdIn(articleIds.toTypedArray()).toList()
        }

        // 타입별 그룹화
        val attachmentsMap = allAssets
            .filter { it.assetType == ArticleAssetType.ATTACHMENT.name }
            .groupBy({ it.articleId }, { it.uid })
        val largeBannersMap = allAssets
            .filter { it.assetType == ArticleAssetType.LARGE_BANNER.name }
            .groupBy({ it.articleId }, { it.uid })
        val smallBannersMap = allAssets
            .filter { it.assetType == ArticleAssetType.SMALL_BANNER.name }
            .groupBy({ it.articleId }, { it.uid })

        val mapped = rows.map { article ->
            if (isEvent(article.category)) {
                ArticleResponse(
                    id = article.id,
                    category = article.category,
                    subject = article.subject,
                    content = article.content?.take(160),
                    link = article.link,
                    hits = article.hits,
                    largeBanners = largeBannersMap[article.id] ?: emptyList(),
                    smallBanners = smallBannersMap[article.id] ?: emptyList(),
                    startDate = article.startDate,
                    endDate = article.endDate,
                    status = article.status,
                    createdAt = article.createdAt,
                    updatedAt = article.updatedAt,
                )
            } else {
                ArticleResponse(
                    id = article.id,
                    category = article.category,
                    subject = article.subject,
                    content = article.content?.take(160),
                    link = article.link,
                    hits = article.hits,
                    attachments = attachmentsMap[article.id] ?: emptyList(),
                    startDate = article.startDate,
                    endDate = article.endDate,
                    status = article.status,
                    createdAt = article.createdAt,
                    updatedAt = article.updatedAt,
                )
            }
        }

        return ApiPage(
            total = total,
            lastPage = lastPage,
            items = mapped,
            perPage = s,
            currentPage = p
        )
    }

    suspend fun listArticlesByCursor(
        category: String?,
        search: String?,
        status: String?,
        size: Int,
        cursorId: Long?,
    ): ApiPageCursor<ArticleResponse> {
        val s = size.coerceIn(1, 100)

        val rows = articles.searchByCursor(category, search, status, s + 1, cursorId).toList()

        val hasMore = rows.size > s
        val items = if (hasMore) rows.dropLast(1) else rows
        val nextCursor = if (hasMore) items.lastOrNull()?.id?.toString() else null

        // 첨부파일 일괄 조회
        val articleIds = items.map { it.id }
        val allAssets = if (articleIds.isEmpty()) {
            emptyList()
        } else {
            articleAssetRepository.findAllByArticleIdIn(articleIds.toTypedArray()).toList()
        }

        // 타입별 그룹화
        val attachmentsMap = allAssets
            .filter { it.assetType == ArticleAssetType.ATTACHMENT.name }
            .groupBy({ it.articleId }, { it.uid })
        val largeBannersMap = allAssets
            .filter { it.assetType == ArticleAssetType.LARGE_BANNER.name }
            .groupBy({ it.articleId }, { it.uid })
        val smallBannersMap = allAssets
            .filter { it.assetType == ArticleAssetType.SMALL_BANNER.name }
            .groupBy({ it.articleId }, { it.uid })

        val mapped = items.map { article ->
            if (isEvent(article.category)) {
                ArticleResponse(
                    id = article.id,
                    category = article.category,
                    subject = article.subject,
                    content = article.content?.take(160),
                    link = article.link,
                    hits = article.hits,
                    largeBanners = largeBannersMap[article.id] ?: emptyList(),
                    smallBanners = smallBannersMap[article.id] ?: emptyList(),
                    startDate = article.startDate,
                    endDate = article.endDate,
                    status = article.status,
                    createdAt = article.createdAt,
                    updatedAt = article.updatedAt,
                )
            } else {
                ArticleResponse(
                    id = article.id,
                    category = article.category,
                    subject = article.subject,
                    content = article.content?.take(160),
                    link = article.link,
                    hits = article.hits,
                    attachments = attachmentsMap[article.id] ?: emptyList(),
                    startDate = article.startDate,
                    endDate = article.endDate,
                    status = article.status,
                    createdAt = article.createdAt,
                    updatedAt = article.updatedAt,
                )
            }
        }

        return ApiPageCursor(
            nextCursor = nextCursor,
            items = mapped,
            perPage = s,
        )
    }
}
