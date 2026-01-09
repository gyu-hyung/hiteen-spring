package kr.jiasoft.hiteen.admin.app

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminGiftResponse
import kr.jiasoft.hiteen.admin.dto.AdminPlayResponse
import kr.jiasoft.hiteen.admin.infra.AdminGiftRepository
import kr.jiasoft.hiteen.admin.infra.AdminPlayRepository
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.gift.domain.GiftCategory
import kr.jiasoft.hiteen.feature.gift.domain.GiftType
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/admin/gift")
class AdminGiftController(
    private val repository: AdminGiftRepository,
    private val assetService: AssetService,
) {

    /**
     * 게시글 등록 / 수정
     */
//    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
//    suspend fun saveBoard(
//        @RequestPart("req") req: AdminBoardCreateRequest,
//        @Parameter(description = "첨부 이미지")
////        @RequestPart(name = "file", required = false) file: FilePart?,
//        @AuthenticationPrincipal(expression = "user") user: UserEntity,
//    ): ResponseEntity<ApiResult<BoardEntity>> {
//
////        val asset = file?.let {
////            assetService.uploadImage(it, user.id, AssetCategory.BOARD)
////        }
//
//        val result = if (req.id == null) {
//            // ✅ 등록
//            val entity = BoardEntity(
//                category = req.category,
//                subject = req.subject,
//                content = req.content,
//                link = req.link,
//                ip = req.ip,
////                assetUid = asset?.uid,
//                startDate = req.startDate,
//                endDate = req.endDate,
//                status = req.status,
//                address = req.address,
//                detailAddress = req.detailAddress,
//                lat = req.lat,
//                lng = req.lng,
//                createdId = user.id,
//                createdAt = OffsetDateTime.now(),
//            )
//
//            adminBoardRepository.save(entity)
//
//        } else {
//            // ✅ 수정
//            val origin = adminBoardRepository.findById(req.id)
//                ?: throw IllegalArgumentException("존재하지 않는 게시글입니다. id=${req.id}")
//
//            val updated = origin.copy(
//                category = req.category,
//                subject = req.subject,
//                content = req.content,
//                link = req.link,
////                assetUid = asset?.uid ?: origin.assetUid,
//                startDate = req.startDate,
//                endDate = req.endDate,
//                status = req.status,
//                address = req.address,
//                detailAddress = req.detailAddress,
//                lat = req.lat,
//                lng = req.lng,
//                updatedId = user.id,
//                updatedAt = OffsetDateTime.now(),
//            )
//
//            adminBoardRepository.save(updated)
//        }
//
//        return ResponseEntity.ok(ApiResult.success(result))
//    }

    /**
     * 게시글 삭제 (Soft Delete)
     */
//    @DeleteMapping
//    suspend fun deleteBoard(
//        @RequestParam id: Long,
//        @AuthenticationPrincipal(expression = "user") user: UserEntity,
//    ): ResponseEntity<ApiResult<Any>> {
//
//        val origin = adminBoardRepository.findById(id)
//            ?: throw IllegalArgumentException("존재하지 않는 게시글입니다. id=$id")
//
//        val deleted = origin.copy(
//            deletedId = user.id,
//            deletedAt = OffsetDateTime.now(),
//        )
//
//        adminBoardRepository.save(deleted)
//
//        return ResponseEntity.ok(ApiResult.success(origin))
//    }

    /**
     * 목록 조회
     */
    @GetMapping
    suspend fun list(
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "DESC",
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam status: String? = null,

        @RequestParam uid: UUID? = null,

        @RequestParam category: GiftCategory? = null,
        @RequestParam type: GiftType? = null,

        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPage<AdminGiftResponse>>> {

        val list = repository.listByPage(
            page = page,
            size = size,
            order = order,
            search = search,
            searchType = searchType,
            status = status,
            uid = uid,
            category = category,
            type = type,
        ).toList()

        val totalCount = repository.totalCount(
            search = search,
            searchType = searchType,
            status = status,
            uid = uid,
            category = category,
            type = type,
        )

        return ResponseEntity.ok(
            ApiResult.success(PageUtil.of(list, totalCount, page, size))
        )
    }
}
