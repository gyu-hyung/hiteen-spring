package kr.jiasoft.hiteen.admin.app

import io.swagger.v3.oas.annotations.Parameter
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminBoardCreateRequest
import kr.jiasoft.hiteen.admin.infra.AdminBoardRepository
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.board.domain.BoardEntity
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/admin/board")
class AdminBoardController(
    private val adminBoardRepository: AdminBoardRepository,
    private val assetService: AssetService,
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
     * 게시글 목록 조회
     */
    @GetMapping
    suspend fun getBoards(
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "DESC",
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam status: String? = null,
        @RequestParam category: String? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPage<BoardEntity>>> {

        val list = adminBoardRepository.listByPage(
            page = page,
            size = size,
            order = order,
            search = search,
            searchType = searchType,
            status = status,
            category = category,
        ).toList()

        val totalCount = adminBoardRepository.totalCount(
            search = search,
            searchType = searchType,
            status = status,
            category = category,
        )

        return ResponseEntity.ok(
            ApiResult.success(PageUtil.of(list, totalCount, page, size))
        )
    }
}
