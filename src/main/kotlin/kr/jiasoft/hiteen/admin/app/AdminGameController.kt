package kr.jiasoft.hiteen.admin.app

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.infra.AdminGameRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.play.domain.GameEntity
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import java.lang.IllegalArgumentException
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/admin/games")
class AdminGameController (
    private val adminGameRepository: AdminGameRepository,
    private val assetService: AssetService,
){

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun saveGoods(
        @RequestPart("req") req: GameEntity,
//        @Parameter(description = "첨부 파일") @RequestPart(name = "file", required = false) file: FilePart?,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<GameEntity>?> {

//        val sAsset =  file?.let { assetService.uploadImage(it, user.id, AssetCategory.GOODS) }

        val res =  if (req.id.toInt() == 0) {
            adminGameRepository.save(req)
        } else {
            val origin = adminGameRepository.findById(req.id)
                ?: throw IllegalArgumentException("존재하지 않는 상품입니다. id=${req.id}")

            val updated = origin.copy(
//                code = req.code,
                name = req.name,
                description = req.description,
                updatedAt = OffsetDateTime.now(),
            )

            adminGameRepository.save(updated)
        }

        return ResponseEntity.ok(ApiResult.success(res))
    }


    @DeleteMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun deleteGoods(
        @RequestParam id: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Any>?>? {

        val origin = adminGameRepository.findById(id)
            ?: throw IllegalArgumentException("존재하지 않는 상품입니다. id=${id}")

        val updated = origin.copy(
            deletedAt = OffsetDateTime.now(),
        )

        adminGameRepository.save(updated)

        return ResponseEntity.ok(ApiResult.success(origin))
    }


    @GetMapping
    suspend fun getGoods(
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "DESC",
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam status: String? = null,
        @RequestParam id: Long? = null,
        @RequestParam uid: String? = null,

        // ⭐ 추가
//        @RequestParam categorySeq: Int? = null,
//        @RequestParam goodsTypeCd: String? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPage<GameEntity?>>> {

        // 1) 목록 조회
        val list = adminGameRepository.listByPage(
            page = page,
            size = size,
            order = order,
            search = search,
            searchType = searchType,
            status = status,
//            categorySeq = categorySeq,
//            goodsTypeCd = goodsTypeCd,
        ).toList()

        // 2) 전체 개수 조회
        val totalCount = adminGameRepository.totalCount(
            search = search,
            searchType = searchType,
            status = status,

//            categorySeq = categorySeq,
//            goodsTypeCd = goodsTypeCd,
        )

        return ResponseEntity.ok(ApiResult.success(PageUtil.of(list, totalCount, page, size)))
    }

}