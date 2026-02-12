package kr.jiasoft.hiteen.feature.gift.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.GoodsCategoryDto
import kr.jiasoft.hiteen.admin.dto.GoodsTypeDto
import kr.jiasoft.hiteen.common.dto.ApiPageCursor
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.gift.dto.GiftBuyRequest
import kr.jiasoft.hiteen.feature.gift.dto.GiftIssueRequest
import kr.jiasoft.hiteen.feature.gift.dto.GiftResponse
import kr.jiasoft.hiteen.feature.gift.dto.GiftUseRequest
import kr.jiasoft.hiteen.feature.gift.infra.GiftRepository
import kr.jiasoft.hiteen.feature.gift.infra.GiftUserRepository
import kr.jiasoft.hiteen.feature.giftishow.domain.GoodsGiftishowEntity
import kr.jiasoft.hiteen.feature.giftishow.infra.GiftishowGoodsRepository
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID


@RestController
@RequestMapping("/api/gift")
@Tag(name = "Gift API", description = "선물 생성 및 조회 API")
class GiftController (
    private val giftAppService: GiftAppService,
    private val giftishowGoodsRepository: GiftishowGoodsRepository,
    private val giftRepository: GiftRepository,
    private val giftUserRepository: GiftUserRepository,
    private val assetService: AssetService,
){


    @GetMapping()
    suspend fun findGift(
        giftUserId: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<ApiResult<GiftResponse?>> {
        return ResponseEntity.ok(ApiResult.success(giftAppService.findGift(user.id, giftUserId)))
    }

    /**
     * 사용자 -> 상품 구매
     * */
    @PostMapping("/buy")
    suspend fun buyGift(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        request: GiftBuyRequest
    ): ResponseEntity<ApiResult<List<GiftResponse>>> {
        return ResponseEntity.ok(ApiResult.success(giftAppService.buyGift(user.id, user.uid, request)))
    }


    /**
     * 사용자 -> 선물 발급
     * */
    @PostMapping("/issue")
    suspend fun issueGift(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        request: GiftIssueRequest
    ): ResponseEntity<ApiResult<GiftResponse>> {
        return ResponseEntity.ok(ApiResult.success(giftAppService.issueGift(user.id, request)))
    }

    /**
     * 사용자 -> 선물 사용
     * */
    @PostMapping("/use")
    suspend fun useGift(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        request: GiftUseRequest
    ): ResponseEntity<ApiResult<GiftResponse>> {
        return ResponseEntity.ok(ApiResult.success(giftAppService.useGift(user.id, request)))
    }


    /**
     * 선물함 목록조회 (커서 기반)
     */
    @GetMapping("/myGiftList")
    suspend fun myGiftList(
        @RequestParam size: Int = 10,
        @RequestParam(required = false) lastId: Long?,   // ⭐ 커서
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<ApiResult<ApiPageCursor<GiftResponse>>> {

        val list = giftAppService.listGiftByCursor(
            userId = user.id,
            size = size,
            lastId = lastId,
        )

        val nextCursor = list.lastOrNull()?.giftUserId?.toString()

        return ResponseEntity.ok(
            ApiResult.success(
                ApiPageCursor(
                    items = list,
                    nextCursor = nextCursor,
                    perPage = size
                )
            )
        )
    }


//    /**
//     * 기프트쇼 상품 목록조회
//     * */
//    @GetMapping("/goods")
//    suspend fun getGifts(): ResponseEntity<ApiResult<List<GoodsGiftishowEntity>>> =
//        ResponseEntity.ok(ApiResult.success(giftAppService.listGoods()))



    /**
     * 상품 카테고리
     * */
    @GetMapping("/goods/categories")
    suspend fun getGoodsCategories(): ResponseEntity<ApiResult<List<GoodsCategoryDto>>> {
        val list = giftishowGoodsRepository.findCategories().toList()
        return ResponseEntity.ok(ApiResult.success(list))
    }


    /**
     * 상품 종류
     * */
    @GetMapping("/goods/types")
    suspend fun getGoodsTypes(): ResponseEntity<ApiResult<List<GoodsTypeDto>>> {
        val list = giftishowGoodsRepository.findGoodsTypes().toList()
        return ResponseEntity.ok(ApiResult.success(list))
    }

    /**
     * 기프트쇼 상품 목록조회
     * */
    @GetMapping("/goods")
    @Operation(description = "ex) /api/gift/goods?lastId=1852&lastBrandName=다이소&lastSalePrice=10000 ")
    suspend fun getGoods(
        @RequestParam size: Int = 10,
        @RequestParam(required = false) lastId: Long?,   // 커서
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam categorySeq: Int? = null,
        @RequestParam goodsTypeCd: String? = null,
        @RequestParam(required = false) brandCode: String? = null,
        @RequestParam(required = false) goodsCodeType: String? = null,
        @RequestParam(required = false) lastBrandName: String? = null,// 커서
        @RequestParam(required = false) lastSalePrice: Int? = null,// 커서
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPageCursor<GoodsGiftishowEntity>>> {

        val list = giftishowGoodsRepository.listByCursorId(
            size = size,
            lastId = lastId,
            lastBrandName = lastBrandName,
            lastSalePrice = lastSalePrice,
            search = search,
            searchType = searchType,
            categorySeq = categorySeq,
            goodsTypeCd = goodsTypeCd,
            brandCode = brandCode,
            goodsCodeType = goodsCodeType,
        ).toList()

        // 다음 커서 (brandName:salePrice:id 형태)
        val lastItem = list.lastOrNull()
        val nextCursor = lastItem?.let { "${it.brandName}:${it.salePrice}:${it.id}" }

        return ResponseEntity.ok(
            ApiResult.success(
                ApiPageCursor(
                    items = list,
                    nextCursor = nextCursor,
                    perPage = size
                )
            )
        )
    }


    /**
     * 바코드 이미지 보안 조회
     * - gift uid와 로그인 사용자 ID로 본인 소유의 giftUser를 조회
     * - couponImg(바코드 asset uid)가 본인 소유인지 검증
     */
    @Operation(
        summary = "바코드 이미지 조회",
        description = "본인 소유의 기프티쇼 바코드 이미지만 조회할 수 있습니다."
    )
    @GetMapping("/barcode/{giftUid}")
    suspend fun getBarcodeImage(
        @PathVariable giftUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<FileSystemResource> {
        // 1 gift 조회
        val gift = giftRepository.findByUid(giftUid)
            ?: return ResponseEntity.notFound().build()

        // 2 giftUser 조회 (gift.id + user.id 조합)
        val giftUser = giftUserRepository.findByGiftIdAndUserId(gift.id, user.id)
            ?: return ResponseEntity.notFound().build()

        // 3 couponImg(바코드 asset uid) 확인
        val barcodeUidStr = giftUser.couponImg
            ?: return ResponseEntity.notFound().build()

        val barcodeUid = try {
            UUID.fromString(barcodeUidStr)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.notFound().build()
        }

        // 4 Asset 조회
        val asset = assetService.findByUid(barcodeUid)
            ?: return ResponseEntity.notFound().build()

        // 5 파일 경로 확인
        val path = assetService.resolveFilePath(asset.filePath + asset.storeFileName)
        if (!assetService.existsFile(path)) {
            return ResponseEntity.notFound().build()
        }

        // 6 파일 반환
        val resource = FileSystemResource(path)
        val mime = asset.type ?: MediaType.IMAGE_PNG_VALUE

        val headers = HttpHeaders().apply {
            contentType = MediaType.parseMediaType(mime)
            contentLength = resource.contentLength()
            add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${asset.originFileName}\"")
        }

        return ResponseEntity.ok()
            .headers(headers)
            .body(resource)
    }


}
