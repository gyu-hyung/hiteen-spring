package kr.jiasoft.hiteen.admin.app

import io.swagger.v3.oas.annotations.Parameter
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminBoardCreateRequest
import kr.jiasoft.hiteen.admin.dto.AdminBoardListResponse
import kr.jiasoft.hiteen.admin.dto.AdminPinResponse
import kr.jiasoft.hiteen.admin.dto.AdminPinSaveRequest
import kr.jiasoft.hiteen.admin.infra.AdminPinRepository
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.board.domain.BoardEntity
import kr.jiasoft.hiteen.feature.pin.domain.PinEntity
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/admin/pin")
class AdminPinController(
    private val adminPinRepository: AdminPinRepository,
    private val assetService: AssetService,
) {

    /**
     * 게시글 등록 / 수정
     */
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun save(
        @RequestPart("req") req: AdminPinSaveRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<PinEntity>> {


        val result = if (req.id == null) {
            val entity = PinEntity(
                userId = req.userId!!,
                zipcode = req.zipcode,
                lat = req.lat!!,
                lng = req.lng!!,
                description = req.description!!,
                visibility = req.visibility!!,
                createdId = user.id,
                createdAt = OffsetDateTime.now(),
            )

            adminPinRepository.save(entity)

        } else {
            // ✅ 수정
            val origin = adminPinRepository.findById(req.id)
                ?: throw IllegalArgumentException("존재하지 않는 게시글입니다. id=${req.id}")

            val updated = origin.copy(
                userId = req.userId?: origin.userId,
                zipcode = req.zipcode?: origin.zipcode,
                lat = req.lat?: origin.lat,
                lng = req.lng?: origin.lng,
                description = req.description?: origin.description,
                visibility = req.visibility?: origin.visibility,
                updatedId = user.id,
                updatedAt = OffsetDateTime.now(),
            )

            adminPinRepository.save(updated)
        }

        return ResponseEntity.ok(ApiResult.success(result))
    }

    /**
     * 게시글 삭제 (Soft Delete)
     */
    @DeleteMapping
    suspend fun delete(
        @RequestParam id: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Any>> {

        val origin = adminPinRepository.findById(id)
            ?: throw IllegalArgumentException("존재하지 않는 게시글입니다. id=$id")

        val deleted = origin.copy(
            deletedId = user.id,
            deletedAt = OffsetDateTime.now(),
        )

        adminPinRepository.save(deleted)

        return ResponseEntity.ok(ApiResult.success(origin))
    }

    /**
     * 게시글 목록 조회
     */
    @GetMapping
    suspend fun getList(
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "DESC",
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam status: String? = null,

        @RequestParam uid: UUID? = null,
        @RequestParam visibility: String? = null,

        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPage<AdminPinResponse>>> {

        val list = adminPinRepository.listByPage(
            page = page,
            size = size,
            order = order,
            search = search,
            searchType = searchType,
            status = status,
            uid = uid,
            visibility = visibility,
        ).toList()

        val totalCount = adminPinRepository.totalCount(
            search = search,
            searchType = searchType,
            status = status,
            uid = uid,
            visibility = visibility,
        )

        return ResponseEntity.ok(
            ApiResult.success(PageUtil.of(list, totalCount, page, size))
        )
    }
}
