package kr.jiasoft.hiteen.admin.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitSingle
import kr.jiasoft.hiteen.admin.dto.AdminArticleListResponse
import kr.jiasoft.hiteen.admin.dto.AdminArticleSaveRequest
import kr.jiasoft.hiteen.admin.infra.AdminArticleRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.article.domain.ArticleAssetEntity
import kr.jiasoft.hiteen.feature.article.domain.ArticleAssetType
import kr.jiasoft.hiteen.feature.article.domain.ArticleCategory
import kr.jiasoft.hiteen.feature.article.domain.ArticleEntity
import kr.jiasoft.hiteen.feature.article.infra.ArticleAssetRepository
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.asset.domain.AssetCategory
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import java.time.OffsetDateTime
import java.util.UUID

@Tag(name = "AdminArticle", description = "관리자 공지사항/이벤트 관련 API")
@RestController
@RequestMapping("/api/admin/article")
@SecurityRequirement(name = "bearerAuth")
class AdminArticleController(
    private val adminArticleRepository: AdminArticleRepository,
    private val articleAssetRepository: ArticleAssetRepository,
    private val assetService: AssetService,
) {

    private fun ArticleEntity.toAdminResponse(
        attachments: List<UUID> = emptyList(),
        largeBanners: List<UUID>? = null,
        smallBanners: List<UUID>? = null,
    ) = AdminArticleListResponse(
        id = this.id,
        category = this.category,
        subject = this.subject,
        content = this.content,
        link = this.link,
        ip = this.ip,
        hits = this.hits,
        startDate = this.startDate,
        endDate = this.endDate,
        status = this.status,
        createdId = this.createdId,
        createdAt = this.createdAt,
        updatedId = this.updatedId,
        updatedAt = this.updatedAt,
        deletedId = this.deletedId,
        deletedAt = this.deletedAt,
        attachments = attachments,
        largeBanners = largeBanners,
        smallBanners = smallBanners,
    )

    private fun isEvent(category: String) = category == ArticleCategory.EVENT.name

    /**
     * 공지사항/이벤트 목록 조회 (페이지 기반) - 관리자 전용
     */
    @Operation(summary = "공지사항/이벤트 목록 조회", description = "관리자용 공지사항/이벤트 목록을 페이지 기반으로 조회합니다.")
    @GetMapping
    suspend fun listByPage(
        @Parameter(description = "페이지 번호 (1부터 시작)") @RequestParam page: Int = 1,
        @Parameter(description = "페이지당 개수") @RequestParam size: Int = 10,
        @Parameter(description = "정렬 순서 (ASC/DESC)") @RequestParam order: String = "DESC",
        @Parameter(description = "검색어") @RequestParam search: String? = null,
        @Parameter(description = "검색 타입 (ALL/subject/content)") @RequestParam searchType: String = "ALL",
        @Parameter(description = "상태 (ACTIVE/INACTIVE/ALL)") @RequestParam status: String? = null,
        @Parameter(description = "노출 상태 (ACTIVE/INACTIVE/ALL)") @RequestParam displayStatus: String? = "ALL",
        @Parameter(description = "카테고리 (NOTICE/EVENT/ALL)") @RequestParam category: String? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPage<AdminArticleListResponse>>> {

        val safePage = page.coerceAtLeast(1)
        val safeSize = size.coerceIn(1, 100)

        val list = adminArticleRepository.listByPage(
            page = safePage,
            size = safeSize,
            order = order,
            search = search,
            searchType = searchType,
            status = status,
            displayStatus = displayStatus,
            category = category,
        ).toList()

        val articleIds = list.map { it.id }

        // 첨부파일 일괄 조회 (타입별 분리)
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

        val listWithAttachments = list.map { row ->
            if (isEvent(row.category)) {
                row.toAdminResponse(
                    attachments = emptyList(),
                    largeBanners = largeBannersMap[row.id] ?: emptyList(),
                    smallBanners = smallBannersMap[row.id] ?: emptyList(),
                )
            } else {
                row.toAdminResponse(
                    attachments = attachmentsMap[row.id] ?: emptyList(),
                )
            }
        }

        val totalCount = adminArticleRepository.totalCount(
            search = search,
            searchType = searchType,
            status = status,
            displayStatus = displayStatus,
            category = category,
        )

        return ResponseEntity.ok(
            ApiResult.success(PageUtil.of(listWithAttachments, totalCount, safePage, safeSize))
        )
    }

    /**
     * 공지사항/이벤트 단건 조회 - 관리자 전용
     */
    @Operation(summary = "공지사항/이벤트 단건 조회", description = "관리자용 공지사항/이벤트 단건을 조회합니다.")
    @GetMapping("/{id}")
    suspend fun getById(
        @Parameter(description = "게시글 ID") @PathVariable id: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<AdminArticleListResponse>> {
        val article = adminArticleRepository.findById(id)
            ?: throw IllegalArgumentException("존재하지 않는 게시글입니다. id=$id")

        val allAssets = articleAssetRepository.findAllByArticleId(article.id).toList()

        return if (isEvent(article.category)) {
            val large = allAssets.filter { it.assetType == ArticleAssetType.LARGE_BANNER.name }.map { it.uid }
            val small = allAssets.filter { it.assetType == ArticleAssetType.SMALL_BANNER.name }.map { it.uid }
            ResponseEntity.ok(ApiResult.success(article.toAdminResponse(largeBanners = large, smallBanners = small)))
        } else {
            val attachments = allAssets.filter { it.assetType == ArticleAssetType.ATTACHMENT.name }.map { it.uid }
            ResponseEntity.ok(ApiResult.success(article.toAdminResponse(attachments = attachments)))
        }
    }

    /**
     * 공지사항/이벤트 등록/수정 (JSON body + 기존 assetUid 연결)
     */
    @Operation(summary = "공지사항/이벤트 등록/수정", description = "관리자용 공지사항/이벤트를 등록 또는 수정합니다.")
    @PostMapping("/save")
    suspend fun save(
        @RequestBody req: AdminArticleSaveRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ArticleEntity>> {
        val isEventCategory = isEvent(req.category)

        val saved = if (req.id == null) {
            adminArticleRepository.save(
                ArticleEntity(
                    category = req.category,
                    subject = req.subject,
                    content = req.content,
                    link = req.link,
                    ip = req.ip,
                    startDate = req.startDate,
                    endDate = req.endDate,
                    status = req.status,
                    createdId = user.id,
                    createdAt = OffsetDateTime.now(),
                )
            )
        } else {
            val origin = adminArticleRepository.findById(req.id)
                ?: throw IllegalArgumentException("존재하지 않는 게시글입니다. id=${req.id}")

            adminArticleRepository.save(
                origin.copy(
                    category = req.category,
                    subject = req.subject,
                    content = req.content,
                    link = req.link,
                    ip = req.ip,
                    startDate = req.startDate,
                    endDate = req.endDate,
                    status = req.status,
                    updatedId = user.id,
                    updatedAt = OffsetDateTime.now(),
                )
            )
        }

        if (isEventCategory) {
            // 이벤트: 배너 처리
            if (req.replaceBanners == true) {
                articleAssetRepository.deleteByArticleIdAndAssetType(saved.id, ArticleAssetType.LARGE_BANNER.name)
                articleAssetRepository.deleteByArticleIdAndAssetType(saved.id, ArticleAssetType.SMALL_BANNER.name)
            } else {
                req.deleteLargeBannerUids?.let { uids ->
                    if (uids.isNotEmpty()) {
                        articleAssetRepository.deleteByArticleIdAndAssetTypeAndUidIn(
                            saved.id, ArticleAssetType.LARGE_BANNER.name, uids.toTypedArray()
                        )
                    }
                }
                req.deleteSmallBannerUids?.let { uids ->
                    if (uids.isNotEmpty()) {
                        articleAssetRepository.deleteByArticleIdAndAssetTypeAndUidIn(
                            saved.id, ArticleAssetType.SMALL_BANNER.name, uids.toTypedArray()
                        )
                    }
                }
            }

            // 큰 배너 저장
            var seq = articleAssetRepository.findMaxSeqByArticleIdAndAssetType(saved.id, ArticleAssetType.LARGE_BANNER.name) + 1
            req.largeBanners?.forEach { uid ->
                try {
                    articleAssetRepository.save(ArticleAssetEntity(
                        articleId = saved.id, uid = uid,
                        assetType = ArticleAssetType.LARGE_BANNER.name, seq = seq++
                    ))
                } catch (_: org.springframework.dao.DuplicateKeyException) {}
            }

            // 작은 배너 저장
            seq = articleAssetRepository.findMaxSeqByArticleIdAndAssetType(saved.id, ArticleAssetType.SMALL_BANNER.name) + 1
            req.smallBanners?.forEach { uid ->
                try {
                    articleAssetRepository.save(ArticleAssetEntity(
                        articleId = saved.id, uid = uid,
                        assetType = ArticleAssetType.SMALL_BANNER.name, seq = seq++
                    ))
                } catch (_: org.springframework.dao.DuplicateKeyException) {}
            }
        } else {
            // 공지사항: 일반 첨부파일 처리
            if (req.replaceAssets == true) {
                articleAssetRepository.deleteByArticleIdAndAssetType(saved.id, ArticleAssetType.ATTACHMENT.name)
            } else {
                req.deleteAssetUids?.let { uids ->
                    if (uids.isNotEmpty()) {
                        articleAssetRepository.deleteByArticleIdAndAssetTypeAndUidIn(
                            saved.id, ArticleAssetType.ATTACHMENT.name, uids.toTypedArray()
                        )
                    }
                }
            }

            req.assetUids?.forEach { uid ->
                try {
                    articleAssetRepository.save(ArticleAssetEntity(
                        articleId = saved.id, uid = uid,
                        assetType = ArticleAssetType.ATTACHMENT.name
                    ))
                } catch (_: org.springframework.dao.DuplicateKeyException) {}
            }
        }

        return ResponseEntity.ok(ApiResult.success(saved))
    }

    /**
     * 공지사항/이벤트 등록 (multipart 업로드 포함)
     * - EVENT: largeFiles/smallFiles 업로드 → article_assets (LARGE_BANNER/SMALL_BANNER)
     * - NOTICE: files 업로드 → article_assets (ATTACHMENT)
     */
    @Operation(summary = "공지사항/이벤트 등록 (파일 업로드 포함)", description = "관리자용 공지사항/이벤트를 파일 업로드와 함께 등록합니다.")
    @PostMapping("/multipart", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun createMultipart(
        @RequestPart("req") req: AdminArticleSaveRequest,
        @RequestPart(name = "files", required = false) filesFlux: Flux<FilePart>?,
        @RequestPart(name = "largeFiles", required = false) largeFilesFlux: Flux<FilePart>?,
        @RequestPart(name = "smallFiles", required = false) smallFilesFlux: Flux<FilePart>?,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Map<String, Any>>> {
        val files = filesFlux?.collectList()?.awaitSingle().orEmpty()
        val largeFiles = largeFilesFlux?.collectList()?.awaitSingle().orEmpty()
        val smallFiles = smallFilesFlux?.collectList()?.awaitSingle().orEmpty()

        val isEventCategory = isEvent(req.category)

        // 1) 파일 업로드
        val uploadedAttachments = if (!isEventCategory && files.isNotEmpty()) {
            assetService.uploadImages(files, user.id, AssetCategory.ARTICLE)
        } else emptyList()

        val uploadedLarge = if (isEventCategory && largeFiles.isNotEmpty()) {
            assetService.uploadImages(largeFiles, user.id, AssetCategory.ARTICLE)
        } else emptyList()

        val uploadedSmall = if (isEventCategory && smallFiles.isNotEmpty()) {
            assetService.uploadImages(smallFiles, user.id, AssetCategory.ARTICLE)
        } else emptyList()

        // 2) articles 저장
        val saved = adminArticleRepository.save(
            ArticleEntity(
                category = req.category,
                subject = req.subject,
                content = req.content,
                link = req.link,
                ip = req.ip,
                startDate = req.startDate,
                endDate = req.endDate,
                status = req.status,
                createdId = user.id,
                createdAt = OffsetDateTime.now(),
            )
        )

        // 3) 첨부/배너 매핑 저장
        if (isEventCategory) {
            // 큰 배너
            var seq = 1
            (uploadedLarge.map { it.uid } + req.largeBanners.orEmpty()).distinct().forEach { uid ->
                try {
                    articleAssetRepository.save(ArticleAssetEntity(
                        articleId = saved.id, uid = uid,
                        assetType = ArticleAssetType.LARGE_BANNER.name, seq = seq++
                    ))
                } catch (_: org.springframework.dao.DuplicateKeyException) {}
            }

            // 작은 배너
            seq = 1
            (uploadedSmall.map { it.uid } + req.smallBanners.orEmpty()).distinct().forEach { uid ->
                try {
                    articleAssetRepository.save(ArticleAssetEntity(
                        articleId = saved.id, uid = uid,
                        assetType = ArticleAssetType.SMALL_BANNER.name, seq = seq++
                    ))
                } catch (_: org.springframework.dao.DuplicateKeyException) {}
            }
        } else {
            // 일반 첨부
            (uploadedAttachments.map { it.uid } + req.assetUids.orEmpty()).distinct().forEach { uid ->
                try {
                    articleAssetRepository.save(ArticleAssetEntity(
                        articleId = saved.id, uid = uid,
                        assetType = ArticleAssetType.ATTACHMENT.name
                    ))
                } catch (_: org.springframework.dao.DuplicateKeyException) {}
            }
        }

        return ResponseEntity.ok(ApiResult.success(mapOf("id" to saved.id)))
    }

    /**
     * 공지사항/이벤트 수정 (multipart 업로드 포함)
     */
    @Operation(summary = "공지사항/이벤트 수정 (파일 업로드 포함)", description = "관리자용 공지사항/이벤트를 파일 업로드와 함께 수정합니다.")
    @PostMapping("/multipart/{id}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun updateMultipart(
        @PathVariable id: Long,
        @RequestPart("req") req: AdminArticleSaveRequest,
        @RequestPart(name = "files", required = false) filesFlux: Flux<FilePart>?,
        @RequestPart(name = "largeFiles", required = false) largeFilesFlux: Flux<FilePart>?,
        @RequestPart(name = "smallFiles", required = false) smallFilesFlux: Flux<FilePart>?,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Unit>> {
        val origin = adminArticleRepository.findById(id)
            ?: throw IllegalArgumentException("존재하지 않는 게시글입니다. id=$id")

        val files = filesFlux?.collectList()?.awaitSingle().orEmpty()
        val largeFiles = largeFilesFlux?.collectList()?.awaitSingle().orEmpty()
        val smallFiles = smallFilesFlux?.collectList()?.awaitSingle().orEmpty()

        val isEventCategory = isEvent(req.category)

        if (isEventCategory) {
            // 이벤트: 배너 처리
            if (req.replaceBanners != false) {
                articleAssetRepository.deleteByArticleIdAndAssetType(origin.id, ArticleAssetType.LARGE_BANNER.name)
                articleAssetRepository.deleteByArticleIdAndAssetType(origin.id, ArticleAssetType.SMALL_BANNER.name)

                val uploadedLarge = if (largeFiles.isNotEmpty()) {
                    assetService.uploadImages(largeFiles, user.id, AssetCategory.ARTICLE)
                } else emptyList()
                val uploadedSmall = if (smallFiles.isNotEmpty()) {
                    assetService.uploadImages(smallFiles, user.id, AssetCategory.ARTICLE)
                } else emptyList()

                val largeUids = (uploadedLarge.map { it.uid } + req.largeBanners.orEmpty()).distinct()
                val smallUids = (uploadedSmall.map { it.uid } + req.smallBanners.orEmpty()).distinct()

                var seq = 1
                largeUids.forEach { uid ->
                    try {
                        articleAssetRepository.save(ArticleAssetEntity(
                            articleId = origin.id, uid = uid,
                            assetType = ArticleAssetType.LARGE_BANNER.name, seq = seq++
                        ))
                    } catch (_: org.springframework.dao.DuplicateKeyException) {}
                }

                seq = 1
                smallUids.forEach { uid ->
                    try {
                        articleAssetRepository.save(ArticleAssetEntity(
                            articleId = origin.id, uid = uid,
                            assetType = ArticleAssetType.SMALL_BANNER.name, seq = seq++
                        ))
                    } catch (_: org.springframework.dao.DuplicateKeyException) {}
                }
            } else {
                // 부분 삭제 + 추가
                req.deleteLargeBannerUids?.let { uids ->
                    if (uids.isNotEmpty()) {
                        articleAssetRepository.deleteByArticleIdAndAssetTypeAndUidIn(
                            origin.id, ArticleAssetType.LARGE_BANNER.name, uids.toTypedArray()
                        )
                    }
                }
                req.deleteSmallBannerUids?.let { uids ->
                    if (uids.isNotEmpty()) {
                        articleAssetRepository.deleteByArticleIdAndAssetTypeAndUidIn(
                            origin.id, ArticleAssetType.SMALL_BANNER.name, uids.toTypedArray()
                        )
                    }
                }

                val uploadedLarge = if (largeFiles.isNotEmpty()) {
                    assetService.uploadImages(largeFiles, user.id, AssetCategory.ARTICLE)
                } else emptyList()
                val uploadedSmall = if (smallFiles.isNotEmpty()) {
                    assetService.uploadImages(smallFiles, user.id, AssetCategory.ARTICLE)
                } else emptyList()

                var nextLargeSeq = articleAssetRepository.findMaxSeqByArticleIdAndAssetType(origin.id, ArticleAssetType.LARGE_BANNER.name) + 1
                (uploadedLarge.map { it.uid } + req.largeBanners.orEmpty()).distinct().forEach { uid ->
                    try {
                        articleAssetRepository.save(ArticleAssetEntity(
                            articleId = origin.id, uid = uid,
                            assetType = ArticleAssetType.LARGE_BANNER.name, seq = nextLargeSeq++
                        ))
                    } catch (_: org.springframework.dao.DuplicateKeyException) {}
                }

                var nextSmallSeq = articleAssetRepository.findMaxSeqByArticleIdAndAssetType(origin.id, ArticleAssetType.SMALL_BANNER.name) + 1
                (uploadedSmall.map { it.uid } + req.smallBanners.orEmpty()).distinct().forEach { uid ->
                    try {
                        articleAssetRepository.save(ArticleAssetEntity(
                            articleId = origin.id, uid = uid,
                            assetType = ArticleAssetType.SMALL_BANNER.name, seq = nextSmallSeq++
                        ))
                    } catch (_: org.springframework.dao.DuplicateKeyException) {}
                }
            }
        } else {
            // 공지사항: 일반 첨부 처리
            if (req.replaceAssets == true) {
                articleAssetRepository.deleteByArticleIdAndAssetType(origin.id, ArticleAssetType.ATTACHMENT.name)
            } else {
                req.deleteAssetUids?.let { uids ->
                    if (uids.isNotEmpty()) {
                        articleAssetRepository.deleteByArticleIdAndAssetTypeAndUidIn(
                            origin.id, ArticleAssetType.ATTACHMENT.name, uids.toTypedArray()
                        )
                    }
                }
            }

            val uploadedAttachments = if (files.isNotEmpty()) {
                assetService.uploadImages(files, user.id, AssetCategory.ARTICLE)
            } else emptyList()

            (uploadedAttachments.map { it.uid } + req.assetUids.orEmpty()).distinct().forEach { uid ->
                try {
                    articleAssetRepository.save(ArticleAssetEntity(
                        articleId = origin.id, uid = uid,
                        assetType = ArticleAssetType.ATTACHMENT.name
                    ))
                } catch (_: org.springframework.dao.DuplicateKeyException) {}
            }
        }

        // 본문 업데이트
        adminArticleRepository.save(
            origin.copy(
                category = req.category,
                subject = req.subject,
                content = req.content,
                link = req.link,
                ip = req.ip,
                startDate = req.startDate,
                endDate = req.endDate,
                status = req.status,
                updatedId = user.id,
                updatedAt = OffsetDateTime.now(),
            )
        )

        return ResponseEntity.ok(ApiResult.success(Unit))
    }

    /**
     * 공지사항/이벤트 상태 변경
     */
    @Operation(
        summary = "공지사항/이벤트 상태 변경",
        description = "게시글의 상태만 변경합니다. (ACTIVE: 진행중, INACTIVE: 비활성, ENDED: 종료, WINNING: 당첨자발표)"
    )
    @PatchMapping("/{id}/status")
    suspend fun updateStatus(
        @Parameter(description = "게시글 ID") @PathVariable id: Long,
        @Parameter(description = "변경할 상태") @RequestParam status: String,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Any>> {
        val validStatuses = listOf("ACTIVE", "INACTIVE", "ENDED", "WINNING")
        if (status !in validStatuses) {
            throw IllegalArgumentException("유효하지 않은 상태입니다. 가능한 값: $validStatuses")
        }

        val origin = adminArticleRepository.findById(id)
            ?: throw IllegalArgumentException("존재하지 않는 게시글입니다. id=$id")

        adminArticleRepository.save(
            origin.copy(
                status = status,
                updatedId = user.id,
                updatedAt = OffsetDateTime.now(),
            )
        )

        return ResponseEntity.ok(ApiResult.success(mapOf("id" to id, "status" to status)))
    }

    /**
     * 공지사항/이벤트 삭제 (Soft Delete)
     */
    @Operation(summary = "공지사항/이벤트 삭제", description = "관리자용 공지사항/이벤트를 삭제합니다 (Soft Delete).")
    @DeleteMapping("/{id}")
    suspend fun delete(
        @Parameter(description = "게시글 ID") @PathVariable id: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Any>> {
        val origin = adminArticleRepository.findById(id)
            ?: throw IllegalArgumentException("존재하지 않는 게시글입니다. id=$id")

        val deleted = origin.copy(
            deletedId = user.id,
            deletedAt = OffsetDateTime.now(),
        )

        adminArticleRepository.save(deleted)

        return ResponseEntity.ok(ApiResult.success(origin))
    }
}
