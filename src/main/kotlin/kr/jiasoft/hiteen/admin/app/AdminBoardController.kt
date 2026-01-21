package kr.jiasoft.hiteen.admin.app

import io.swagger.v3.oas.annotations.Parameter
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminBoardCreateRequest
import kr.jiasoft.hiteen.admin.dto.AdminBoardListResponse
import kr.jiasoft.hiteen.admin.dto.AdminBoardSaveRequest
import kr.jiasoft.hiteen.admin.infra.AdminBoardRepository
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.board.domain.BoardBannerEntity
import kr.jiasoft.hiteen.feature.board.domain.BoardBannerType
import kr.jiasoft.hiteen.feature.board.domain.BoardCategory
import kr.jiasoft.hiteen.feature.board.infra.BoardAssetRepository
import kr.jiasoft.hiteen.feature.board.infra.BoardBannerRepository
import kr.jiasoft.hiteen.feature.board.domain.BoardEntity
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.coroutines.reactive.awaitSingle
import kr.jiasoft.hiteen.feature.asset.domain.AssetCategory
import org.springframework.http.codec.multipart.FilePart
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/api/admin/board")
class AdminBoardController(
    private val adminBoardRepository: AdminBoardRepository,
    private val assetService: AssetService,
    private val boardAssetRepository: BoardAssetRepository,
    private val boardBannerRepository: BoardBannerRepository,
) {

    /**
     * 게시글 등록 / 수정
     */
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun saveBoard(
        @RequestPart("req") req: AdminBoardCreateRequest,
        @Parameter(description = "첨부 이미지")
//        @RequestPart(name = "file", required = false) file: FilePart?,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<BoardEntity>> {

//        val asset = file?.let {
//            assetService.uploadImage(it, user.id, AssetCategory.BOARD)
//        }

        val result = if (req.id == null) {
            // ✅ 등록
            val entity = BoardEntity(
                category = req.category,
                subject = req.subject,
                content = req.content,
                link = req.link,
                ip = req.ip,
//                assetUid = asset?.uid,
                startDate = req.startDate,
                endDate = req.endDate,
                status = req.status,
                address = req.address,
                detailAddress = req.detailAddress,
                lat = req.lat,
                lng = req.lng,
                createdId = user.id,
                createdAt = OffsetDateTime.now(),
            )

            adminBoardRepository.save(entity)

        } else {
            // ✅ 수정
            val origin = adminBoardRepository.findById(req.id)
                ?: throw IllegalArgumentException("존재하지 않는 게시글입니다. id=${req.id}")

            val updated = origin.copy(
                category = req.category,
                subject = req.subject,
                content = req.content,
                link = req.link,
//                assetUid = asset?.uid ?: origin.assetUid,
                startDate = req.startDate,
                endDate = req.endDate,
                status = req.status,
                address = req.address,
                detailAddress = req.detailAddress,
                lat = req.lat,
                lng = req.lng,
                updatedId = user.id,
                updatedAt = OffsetDateTime.now(),
            )

            adminBoardRepository.save(updated)
        }

        return ResponseEntity.ok(ApiResult.success(result))
    }

    /**
     * 게시글 삭제 (Soft Delete)
     */
    @DeleteMapping
    suspend fun deleteBoard(
        @RequestParam id: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Any>> {

        val origin = adminBoardRepository.findById(id)
            ?: throw IllegalArgumentException("존재하지 않는 게시글입니다. id=$id")

        val deleted = origin.copy(
            deletedId = user.id,
            deletedAt = OffsetDateTime.now(),
        )

        adminBoardRepository.save(deleted)

        return ResponseEntity.ok(ApiResult.success(origin))
    }

    /**
     * 게시글 목록 조회 (페이지 기반) - 관리자 전용
     * (기존 getBoards 기능을 listByPage로 정리)
     */
    @GetMapping
    suspend fun listByPage(
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "DESC",
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam status: String? = null,
        @RequestParam displayStatus: String? = "ALL",

        @RequestParam uid: UUID? = null,
        @RequestParam category: String? = null,

        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPage<AdminBoardListResponse>>> {

        val safePage = page.coerceAtLeast(1)
        val safeSize = size.coerceIn(1, 100)

        val list = adminBoardRepository.listByPage(
            page = safePage,
            size = safeSize,
            order = order,
            search = search,
            searchType = searchType,
            status = status,
            displayStatus = displayStatus,
            uid = uid,
            category = category,
        ).toList()

        val boardIds = list.map { it.id }

        val eventBoardIds = list.filter { it.category == BoardCategory.EVENT.name || it.category == BoardCategory.EVENT_WINNING.name }
            .map { it.id }
            .toSet()

        val assetBoardIds = boardIds.filterNot { eventBoardIds.contains(it) }

        val assetAttachmentsMap: Map<Long, List<UUID>> = if (assetBoardIds.isEmpty()) {
            emptyMap()
        } else {
            boardAssetRepository.findAllByBoardIdIn(assetBoardIds.toTypedArray())
                .toList()
                .groupBy({ it.boardId }, { it.uid })
        }

        val bannerAttachmentsMap: Map<Long, List<UUID>> = if (eventBoardIds.isEmpty()) {
            emptyMap()
        } else {
            // boardId별 배너 uid를 seq 순으로 묶음(large/small 혼합)
            eventBoardIds.associateWith { id ->
                boardBannerRepository.findAllByBoardIdOrderByTypeAndSeq(id).toList().map { it.uid }
            }
        }

        val bannerTypeMap: Map<Long, Pair<List<UUID>, List<UUID>>> = if (eventBoardIds.isEmpty()) {
            emptyMap()
        } else {
            eventBoardIds.associateWith { id ->
                val banners = boardBannerRepository.findAllByBoardIdOrderByTypeAndSeq(id).toList()
                val large = banners.filter { it.bannerType == BoardBannerType.LARGE.name }.map { it.uid }
                val small = banners.filter { it.bannerType == BoardBannerType.SMALL.name }.map { it.uid }
                large to small
            }
        }

        val attachmentsMap: Map<Long, List<UUID>> = boardIds.associateWith { id ->
            bannerAttachmentsMap[id] ?: assetAttachmentsMap[id] ?: emptyList()
        }

        val listWithAttachments = list.map { row ->
            val (large, small) = bannerTypeMap[row.id] ?: (emptyList<UUID>() to emptyList())
            row.copy(
                attachments = attachmentsMap[row.id] ?: emptyList(),
                largeBanners = if (row.category == BoardCategory.EVENT.name || row.category == BoardCategory.EVENT_WINNING.name) large else null,
                smallBanners = if (row.category == BoardCategory.EVENT.name || row.category == BoardCategory.EVENT_WINNING.name) small else null,
            )
        }

        val totalCount = adminBoardRepository.totalCount(
            search = search,
            searchType = searchType,
            status = status,
            displayStatus = displayStatus,
            uid = uid,
            category = category,
        )

        return ResponseEntity.ok(
            ApiResult.success(PageUtil.of(listWithAttachments, totalCount, safePage, safeSize))
        )
    }

    /**
     * 게시글 등록/수정 (관리자)
     * - EVENT/EVENT_WINNING: board_banners에 large/small 배너 저장
     * - 그 외: 기존 로직(추후 board_assets 연동)
     */
    @PostMapping("/save")
    suspend fun saveBoardWithBanners(
        @RequestBody req: AdminBoardSaveRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<BoardEntity>> {
        val isEvent = req.category == BoardCategory.EVENT.name || req.category == BoardCategory.EVENT_WINNING.name

        val saved = if (req.id == null) {
            adminBoardRepository.save(
                BoardEntity(
                    category = req.category,
                    subject = req.subject,
                    content = req.content,
                    link = req.link,
                    ip = req.ip,
                    startDate = req.startDate,
                    endDate = req.endDate,
                    status = req.status,
                    address = req.address,
                    detailAddress = req.detailAddress,
                    lat = req.lat,
                    lng = req.lng,
                    createdId = user.id,
                    createdAt = OffsetDateTime.now(),
                )
            )
        } else {
            val origin = adminBoardRepository.findById(req.id)
                ?: throw IllegalArgumentException("존재하지 않는 게시글입니다. id=${req.id}")

            adminBoardRepository.save(
                origin.copy(
                    category = req.category,
                    subject = req.subject,
                    content = req.content,
                    link = req.link,
                    ip = req.ip,
                    startDate = req.startDate,
                    endDate = req.endDate,
                    status = req.status,
                    address = req.address,
                    detailAddress = req.detailAddress,
                    lat = req.lat,
                    lng = req.lng,
                    updatedId = user.id,
                    updatedAt = OffsetDateTime.now(),
                )
            )
        }

        if (isEvent) {
            // 이벤트 계열은 board_banners로 관리
            boardBannerRepository.deleteByBoardId(saved.id)

            val large = req.largeBanners.orEmpty()
            val small = req.smallBanners.orEmpty()

            var seq = 1
            large.forEach { uid ->
                boardBannerRepository.save(
                    BoardBannerEntity(
                        boardId = saved.id,
                        uid = uid,
                        bannerType = BoardBannerType.LARGE.name,
                        seq = seq++,
                    )
                )
            }

            seq = 1
            small.forEach { uid ->
                boardBannerRepository.save(
                    BoardBannerEntity(
                        boardId = saved.id,
                        uid = uid,
                        bannerType = BoardBannerType.SMALL.name,
                        seq = seq++,
                    )
                )
            }
        } else {
            // 기타 카테고리는 board_assets 흐름을 계속 사용(현재 API는 배너만 다룸)
            // 필요 시: BoardController의 multipart + assets 업로드 로직을 이쪽으로 이관
        }

        return ResponseEntity.ok(ApiResult.success(saved))
    }

    /**
     * 게시글 등록(관리자, multipart)
     * - EVENT/EVENT_WINNING: largeFiles/smallFiles 업로드 → board_banners 저장
     * - 그 외: files 업로드 → board_assets 저장
     */
    @PostMapping("/multipart", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun createMultipart(
        @RequestPart("req") req: AdminBoardSaveRequest,
        @RequestPart(name = "files", required = false) filesFlux: Flux<FilePart>?,
        @RequestPart(name = "largeFiles", required = false) largeFilesFlux: Flux<FilePart>?,
        @RequestPart(name = "smallFiles", required = false) smallFilesFlux: Flux<FilePart>?,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Map<String, Any>>> {
        val files = filesFlux?.collectList()?.awaitSingle().orEmpty()
        val largeFiles = largeFilesFlux?.collectList()?.awaitSingle().orEmpty()
        val smallFiles = smallFilesFlux?.collectList()?.awaitSingle().orEmpty()

        val isEvent = req.category == BoardCategory.EVENT.name || req.category == BoardCategory.EVENT_WINNING.name

        // 1) 업로드
        val uploadedForAssets = if (!isEvent && files.isNotEmpty()) {
            assetService.uploadImages(files, user.id, AssetCategory.POST)
        } else emptyList()

        val uploadedLarge = if (isEvent && largeFiles.isNotEmpty()) {
            assetService.uploadImages(largeFiles, user.id, AssetCategory.POST)
        } else emptyList()

        val uploadedSmall = if (isEvent && smallFiles.isNotEmpty()) {
            assetService.uploadImages(smallFiles, user.id, AssetCategory.POST)
        } else emptyList()

        // 2) 대표이미지 규칙(기존 BoardService.create 수준)
        val representativeUid: UUID? = when {
            isEvent -> (uploadedLarge.firstOrNull() ?: uploadedSmall.firstOrNull())?.uid
            else -> uploadedForAssets.firstOrNull()?.uid
        }

        // 3) boards 저장
        val saved = adminBoardRepository.save(
            BoardEntity(
                category = req.category,
                subject = req.subject,
                content = req.content,
                link = req.link,
                ip = req.ip,
                startDate = req.startDate,
                endDate = req.endDate,
                status = req.status,
                address = req.address,
                detailAddress = req.detailAddress,
                lat = req.lat,
                lng = req.lng,
                assetUid = representativeUid,
                createdId = user.id,
                createdAt = OffsetDateTime.now(),
            )
        )

        // 4) 첨부 매핑 저장
        if (isEvent) {
            // 이벤트는 board_banners
            var seq = 1
            uploadedLarge.forEach { a ->
                boardBannerRepository.save(
                    BoardBannerEntity(boardId = saved.id, uid = a.uid, bannerType = BoardBannerType.LARGE.name, seq = seq++)
                )
            }
            seq = 1
            uploadedSmall.forEach { a ->
                boardBannerRepository.save(
                    BoardBannerEntity(boardId = saved.id, uid = a.uid, bannerType = BoardBannerType.SMALL.name, seq = seq++)
                )
            }
        } else {
            // 일반은 board_assets
            uploadedForAssets.forEach { a ->
                boardAssetRepository.save(
                    kr.jiasoft.hiteen.feature.board.domain.BoardAssetEntity(boardId = saved.id, uid = a.uid)
                )
            }
        }

        return ResponseEntity.ok(ApiResult.success(mapOf("id" to saved.id, "uid" to saved.uid)))
    }

    /**
     * 게시글 수정(관리자, multipart)
     * - 일반 게시글: BoardService.update 규칙(replaceAssets/deleteAssetUids/대표이미지) 동일 적용
     * - EVENT/EVENT_WINNING: replaceBanners=true면 board_banners 전체 교체(largeFiles/smallFiles + req.largeBanners/smallBanners)
     */
    @PostMapping("/multipart/{id}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun updateMultipart(
        @PathVariable id: Long,
        @RequestPart("req") req: AdminBoardSaveRequest,
        @RequestPart(name = "files", required = false) filesFlux: Flux<FilePart>?,
        @RequestPart(name = "largeFiles", required = false) largeFilesFlux: Flux<FilePart>?,
        @RequestPart(name = "smallFiles", required = false) smallFilesFlux: Flux<FilePart>?,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Unit>> {
        val origin = adminBoardRepository.findById(id)
            ?: throw IllegalArgumentException("존재하지 않는 게시글입니다. id=$id")

        val files = filesFlux?.collectList()?.awaitSingle().orEmpty()
        val largeFiles = largeFilesFlux?.collectList()?.awaitSingle().orEmpty()
        val smallFiles = smallFilesFlux?.collectList()?.awaitSingle().orEmpty()

        val isEvent = origin.category == BoardCategory.EVENT.name || origin.category == BoardCategory.EVENT_WINNING.name

        if (isEvent) {
            // 이벤트 배너 교체
            if (req.replaceBanners != false) {
                boardBannerRepository.deleteByBoardId(origin.id)

                // 업로드 파일
                val uploadedLarge = if (largeFiles.isNotEmpty()) assetService.uploadImages(largeFiles, user.id, AssetCategory.POST) else emptyList()
                val uploadedSmall = if (smallFiles.isNotEmpty()) assetService.uploadImages(smallFiles, user.id, AssetCategory.POST) else emptyList()

                // uid로 직접 지정한 배너(이미 업로드된 assets)
                val largeUids = (uploadedLarge.map { it.uid } + req.largeBanners.orEmpty()).distinct()
                val smallUids = (uploadedSmall.map { it.uid } + req.smallBanners.orEmpty()).distinct()

                var seq = 1
                largeUids.forEach { uid ->
                    boardBannerRepository.save(
                        BoardBannerEntity(boardId = origin.id, uid = uid, bannerType = BoardBannerType.LARGE.name, seq = seq++)
                    )
                }
                seq = 1
                smallUids.forEach { uid ->
                    boardBannerRepository.save(
                        BoardBannerEntity(boardId = origin.id, uid = uid, bannerType = BoardBannerType.SMALL.name, seq = seq++)
                    )
                }

                // 대표이미지: large > small 우선
                val newRep: UUID? = largeUids.firstOrNull() ?: smallUids.firstOrNull()
                if (newRep != origin.assetUid) {
                    adminBoardRepository.save(
                        origin.copy(assetUid = newRep, updatedId = user.id, updatedAt = OffsetDateTime.now())
                    )
                }
            } else {
                // 부분 삭제 + 추가
                val delLarge = req.deleteLargeBannerUids?.distinct().orEmpty()
                val delSmall = req.deleteSmallBannerUids?.distinct().orEmpty()

                if (delLarge.isNotEmpty()) {
                    boardBannerRepository.deleteByBoardIdAndBannerTypeAndUidIn(origin.id, BoardBannerType.LARGE.name, delLarge.toTypedArray())
                }
                if (delSmall.isNotEmpty()) {
                    boardBannerRepository.deleteByBoardIdAndBannerTypeAndUidIn(origin.id, BoardBannerType.SMALL.name, delSmall.toTypedArray())
                }

                // 업로드/uid 추가
                val uploadedLarge = if (largeFiles.isNotEmpty()) assetService.uploadImages(largeFiles, user.id, AssetCategory.POST) else emptyList()
                val uploadedSmall = if (smallFiles.isNotEmpty()) assetService.uploadImages(smallFiles, user.id, AssetCategory.POST) else emptyList()

                val addLargeUids = (uploadedLarge.map { it.uid } + req.largeBanners.orEmpty()).distinct()
                val addSmallUids = (uploadedSmall.map { it.uid } + req.smallBanners.orEmpty()).distinct()

                // 기존 배너 가져와서 seq의 다음부터 추가
                val existing = boardBannerRepository.findAllByBoardIdOrderByTypeAndSeq(origin.id).toList()
                var nextLargeSeq = (existing.filter { it.bannerType == BoardBannerType.LARGE.name }.maxOfOrNull { it.seq } ?: 0) + 1
                var nextSmallSeq = (existing.filter { it.bannerType == BoardBannerType.SMALL.name }.maxOfOrNull { it.seq } ?: 0) + 1

                addLargeUids.forEach { uid ->
                    try {
                        boardBannerRepository.save(BoardBannerEntity(boardId = origin.id, uid = uid, bannerType = BoardBannerType.LARGE.name, seq = nextLargeSeq++))
                    } catch (_: org.springframework.dao.DuplicateKeyException) {
                    }
                }
                addSmallUids.forEach { uid ->
                    try {
                        boardBannerRepository.save(BoardBannerEntity(boardId = origin.id, uid = uid, bannerType = BoardBannerType.SMALL.name, seq = nextSmallSeq++))
                    } catch (_: org.springframework.dao.DuplicateKeyException) {
                    }
                }

                // 대표 이미지 재계산(large 우선)
                val after = boardBannerRepository.findAllByBoardIdOrderByTypeAndSeq(origin.id).toList()
                val rep = after.firstOrNull { it.bannerType == BoardBannerType.LARGE.name }?.uid
                    ?: after.firstOrNull { it.bannerType == BoardBannerType.SMALL.name }?.uid
                if (rep != origin.assetUid) {
                    adminBoardRepository.save(origin.copy(assetUid = rep, updatedId = user.id, updatedAt = OffsetDateTime.now()))
                }
            }
        } else {
            // 일반 첨부 업데이트 (BoardService.update 규칙)
            val toDelete = req.deleteAssetUids?.distinct().orEmpty()

            if (req.replaceAssets == true) {
                boardAssetRepository.deleteByBoardId(origin.id)
            } else if (toDelete.isNotEmpty()) {
                boardAssetRepository.deleteByBoardIdAndUidIn(origin.id, toDelete)
            }

            val uploadedUids: List<UUID> = if (files.isNotEmpty()) {
                val uploaded = assetService.uploadImages(files, user.id, AssetCategory.POST)
                uploaded.forEach { a ->
                    boardAssetRepository.save(kr.jiasoft.hiteen.feature.board.domain.BoardAssetEntity(boardId = origin.id, uid = a.uid))
                }
                uploaded.map { it.uid }
            } else emptyList()

            val newAssetUid: UUID? = when {
                uploadedUids.isNotEmpty() -> uploadedUids.first()
                req.replaceAssets == true -> null
                else -> boardAssetRepository.findTopUidByBoardIdOrderByIdDesc(origin.id)
            }

            val merged = origin.copy(
                category = req.category,
                subject = req.subject,
                content = req.content,
                link = req.link,
                assetUid = newAssetUid,
                startDate = req.startDate,
                endDate = req.endDate,
                status = req.status,
                address = req.address,
                detailAddress = req.detailAddress,
                lat = req.lat,
                lng = req.lng,
                ip = req.ip,
                updatedId = user.id,
                updatedAt = OffsetDateTime.now(),
            )
            adminBoardRepository.save(merged)
        }

        // 공통 본문 업데이트(이벤트도 내용/기간/상태 등은 수정 가능)
        val finalMerged = adminBoardRepository.findById(origin.id)!!
            .copy(
                category = req.category,
                subject = req.subject,
                content = req.content,
                link = req.link,
                startDate = req.startDate,
                endDate = req.endDate,
                status = req.status,
                address = req.address,
                detailAddress = req.detailAddress,
                lat = req.lat,
                lng = req.lng,
                ip = req.ip,
                updatedId = user.id,
                updatedAt = OffsetDateTime.now(),
            )
        adminBoardRepository.save(finalMerged)

        return ResponseEntity.ok(ApiResult.success(Unit))
    }
}
