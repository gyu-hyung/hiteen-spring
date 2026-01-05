package kr.jiasoft.hiteen.admin.app

import io.swagger.v3.oas.annotations.Parameter
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.GoodsCategoryDto
import kr.jiasoft.hiteen.admin.dto.GoodsGiftishowCreateRequest
import kr.jiasoft.hiteen.admin.dto.GoodsTypeDto
import kr.jiasoft.hiteen.admin.infra.AdminGoodsRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.asset.domain.AssetCategory
import kr.jiasoft.hiteen.feature.giftishow.domain.GoodsGiftishowEntity
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
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
@RequestMapping("/api/admin/goods")
class AdminGoodsController (
    private val adminGoodsRepository: AdminGoodsRepository,
    private val assetService: AssetService,
){


    private suspend fun generateNextGoodsCode(): String {
        val maxCode = adminGoodsRepository.findMaxHGoodsCode()

        val nextNumber = maxCode
            ?.substring(1)            // "00000000001"
            ?.toLongOrNull()
            ?.plus(1)
            ?: 1L                     // 최초 생성 시

        return "H" + nextNumber.toString().padStart(11, '0')
    }


    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun saveGoods(
        @RequestPart("req") req: GoodsGiftishowCreateRequest,
        @Parameter(description = "첨부 파일 small") @RequestPart(name = "fileS", required = false) fileS: FilePart?,
        @Parameter(description = "첨부 파일 large") @RequestPart(name = "fileB", required = false) fileB: FilePart?,
        @Parameter(description = "첨부 파일 brand") @RequestPart(name = "fileBrand", required = false) fileBrand: FilePart?,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<GoodsGiftishowEntity>?>? {

        val sAsset =  fileS?.let { assetService.uploadImage(it, user.id, AssetCategory.GOODS) }
        val bAsset =  fileB?.let { assetService.uploadImage(it, user.id, AssetCategory.GOODS) }
        val brandAsset =  fileBrand?.let { assetService.uploadImage(it, user.id, AssetCategory.GOODS) }

        val res =  if (req.id == null) {

            val entity = GoodsGiftishowEntity(
                goodsNo = req.goodsNo,
                goodsCode = generateNextGoodsCode(),
                goodsName = req.goodsName,
                brandCode = req.brandCode,
                brandName = req.brandName,
                content = req.content,
                contentAddDesc = req.contentAddDesc,
                searchKeyword = req.searchKeyword,
                mdCode = req.mdCode,
                category1Seq = req.category1Seq,
                category1Name = req.category1Name,
                goodsTypeCode = req.goodsTypeCode,
                goodsTypeName = req.goodsTypeName,
                goodsTypeDetailName = req.goodsTypeDetailName,
                goodsImgS = sAsset?.uid?.toString(),
                goodsImgB = bAsset?.uid?.toString(),
                goodsDescImgWeb = req.goodsDescImgWeb,
                brandIconImg = brandAsset?.uid?.toString(),
                mmsGoodsImg = req.mmsGoodsImg,
                salePrice = req.salePrice,
                realPrice = req.realPrice,
                discountRate = req.discountRate,
                discountPrice = req.discountPrice,
                goodsComId = req.goodsComId,
                goodsComName = req.goodsComName,
                validPeriodType = req.validPeriodType,
                limitDay = req.limitDay,
                validPeriodDay = req.validPeriodDay,
                goodsStateCode = req.goodsStateCode,
                status = req.status,
            )

            adminGoodsRepository.save(entity)
        } else {
            // ✅ 수정
            val origin = adminGoodsRepository.findById(req.id)
                ?: throw IllegalArgumentException("존재하지 않는 상품입니다. id=${req.id}")

            val updated = origin.copy(
                goodsName = req.goodsName,
                brandCode = req.brandCode,
                brandName = req.brandName,
                content = req.content,
                contentAddDesc = req.contentAddDesc,
                searchKeyword = req.searchKeyword,
                mdCode = req.mdCode,
                category1Seq = req.category1Seq,
                category1Name = req.category1Name,
                goodsTypeCode = req.goodsTypeCode,
                goodsTypeName = req.goodsTypeName,
                goodsTypeDetailName = req.goodsTypeDetailName,
                goodsImgS = sAsset?.uid?.toString() ?: origin.goodsImgS,
                goodsImgB = bAsset?.uid?.toString() ?: origin.goodsImgB,
                goodsDescImgWeb = req.goodsDescImgWeb,
                brandIconImg = brandAsset?.uid?.toString() ?: origin.brandIconImg,
                mmsGoodsImg = req.mmsGoodsImg,
                salePrice = req.salePrice,
                realPrice = req.realPrice,
                discountRate = req.discountRate,
                discountPrice = req.discountPrice,
                goodsComId = req.goodsComId,
                goodsComName = req.goodsComName,
                validPeriodType = req.validPeriodType,
                limitDay = req.limitDay,
                validPeriodDay = req.validPeriodDay,
                goodsStateCode = req.goodsStateCode,
                status = req.status,
                updatedAt = OffsetDateTime.now(),
            )

            adminGoodsRepository.save(updated)
        }

        return ResponseEntity.ok(ApiResult.success(res))
    }


    @DeleteMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun deleteGoods(
        @RequestParam id: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Any>?>? {

        val origin = adminGoodsRepository.findById(id)
            ?: throw IllegalArgumentException("존재하지 않는 상품입니다. id=${id}")

        val updated = origin.copy(
            deletedAt = OffsetDateTime.now(),
        )

        adminGoodsRepository.save(updated)

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
        @RequestParam categorySeq: Int? = null,
        @RequestParam goodsTypeCd: String? = null,


        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPage<GoodsGiftishowEntity>>> {

        // 1) 목록 조회
        val list = adminGoodsRepository.listByPage(
            page = page,
            size = size,
            order = order,
            search = search,
            searchType = searchType,
            status = status,
            categorySeq = categorySeq,
            goodsTypeCd = goodsTypeCd,
        ).toList()

        // 2) 전체 개수 조회
        val totalCount = adminGoodsRepository.totalCount(
            search = search,
            searchType = searchType,
            status = status,

            categorySeq = categorySeq,
            goodsTypeCd = goodsTypeCd,
        )

        return ResponseEntity.ok(ApiResult.success(PageUtil.of(list, totalCount, page, size)))
    }


    @GetMapping("/categories")
    suspend fun getGoodsCategories(): ResponseEntity<ApiResult<List<GoodsCategoryDto>>> {
        val list = adminGoodsRepository.findCategories().toList()
        println("list = ${list}")
        return ResponseEntity.ok(ApiResult.success(list))
    }



    @GetMapping("/types")
    suspend fun getGoodsTypes(): ResponseEntity<ApiResult<List<GoodsTypeDto>>> {
        val list = adminGoodsRepository.findGoodsTypes().toList()
        println("list = ${list}")
        return ResponseEntity.ok(ApiResult.success(list))
    }


}