package kr.jiasoft.hiteen.admin.app

import io.swagger.v3.oas.annotations.Operation
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminGiftResponse
import kr.jiasoft.hiteen.admin.dto.AdminGiftStatusUpdateRequest
import kr.jiasoft.hiteen.admin.dto.AdminUserResponse
import kr.jiasoft.hiteen.admin.dto.GoodsBrandDto
import kr.jiasoft.hiteen.admin.dto.GoodsCategoryDto
import kr.jiasoft.hiteen.admin.dto.GoodsTypeDto
import kr.jiasoft.hiteen.admin.infra.AdminGiftRepository
import kr.jiasoft.hiteen.admin.infra.AdminUserRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiPageCursor
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.gift.app.GiftAppService
import kr.jiasoft.hiteen.feature.gift.app.GiftshowClient
import kr.jiasoft.hiteen.feature.gift.domain.GiftCategory
import kr.jiasoft.hiteen.feature.gift.domain.GiftType
import kr.jiasoft.hiteen.feature.gift.dto.GiftProvideRequest
import kr.jiasoft.hiteen.feature.gift.dto.GiftResponse
import kr.jiasoft.hiteen.feature.gift.dto.GiftStatus
import kr.jiasoft.hiteen.feature.gift.dto.client.GiftishowApiResponse
import kr.jiasoft.hiteen.feature.gift.dto.client.biz.GiftishowBizMoneyResult
import kr.jiasoft.hiteen.feature.gift.infra.GiftRepository
import kr.jiasoft.hiteen.feature.gift.infra.GiftUserRepository
import kr.jiasoft.hiteen.feature.giftishow.domain.GoodsGiftishowEntity
import kr.jiasoft.hiteen.feature.giftishow.infra.GiftishowGoodsRepository
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/admin/gift")
@PreAuthorize("hasRole('ADMIN')")
class AdminGiftController(
    private val repository: AdminGiftRepository,
    private val giftRepository: GiftRepository,
    private val giftAppService: GiftAppService,
    private val giftishowGoodsRepository: GiftishowGoodsRepository,
    private val giftshowClient: GiftshowClient,
    private val giftUserRepository: GiftUserRepository,
    private val assetService: AssetService,
) {

    /**
     * 관리자 -> 사용자 선물 생성(지급)
     * 기존: POST /api/gift/admin/create
     */
    @PostMapping("/create")
    suspend fun createGift(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestBody request: GiftProvideRequest,
    ): ResponseEntity<ApiResult<List<GiftResponse>>> {
        return ResponseEntity.ok(ApiResult.success(giftAppService.createGift(user.id, request)))
    }


    /**
     * 기프트쇼 비즈머니 조회
     */
    @GetMapping("/giftishow/bizmoney")
    suspend fun getBizMoney(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<GiftishowApiResponse<GiftishowBizMoneyResult>>> {
        return ResponseEntity.ok(ApiResult.success(giftshowClient.bizMoney()))
    }


    /**
     * 선물 삭제 (관리자용)
     * - giftType 이 Voucher 인 경우 기프티쇼 취소 API를 호출합니다.
     */
    @PostMapping("/delete")
    suspend fun deleteGift(
        @RequestParam giftUid: UUID,
        @RequestParam giftUserId: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Any?>> {
        val res = giftAppService.deleteGift(giftUid, giftUserId)
        return ResponseEntity.ok(ApiResult.success(res))
    }


    /**
     * 선물 상태 변경 (관리자용)
     * - Delivery: 배송요청(4) → 배송완료(5)
     * - GiftCard: 지급요청(6) → 지급완료(7)
     */
    @Operation(
        summary = "선물 상태 변경 (관리자용)",
        description = "배송상품/기프트카드의 상태를 변경합니다. (배송완료, 지급완료 등)"
    )
    @PutMapping("/status")
    suspend fun updateGiftStatus(
        @RequestBody req: AdminGiftStatusUpdateRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<AdminGiftResponse>> {
        // 1️⃣ gift 조회
        val gift = giftRepository.findByUid(req.giftUid)
            ?: throw IllegalArgumentException("존재하지 않는 선물입니다.")

        // 2️⃣ giftType 검증 (Delivery, GiftCard만 허용)
        if (gift.type !in listOf(GiftType.Delivery, GiftType.GiftCard)) {
            throw IllegalArgumentException("배송상품(Delivery) 또는 기프트카드(GiftCard)만 상태 변경이 가능합니다.")
        }

        // 3️⃣ 상태 코드 검증
        val allowedStatuses = when (gift.type) {
            GiftType.Delivery -> listOf(
                GiftStatus.DELIVERY_REQUESTED.code,
                GiftStatus.DELIVERY_DONE.code
            )
            GiftType.GiftCard -> listOf(
                GiftStatus.GRANT_REQUESTED.code,
                GiftStatus.GRANTED.code
            )
            else -> emptyList()
        }

        if (req.status !in allowedStatuses) {
            throw IllegalArgumentException("허용되지 않는 상태 코드입니다: ${req.status}")
        }

        // 4️⃣ giftUser 조회 및 상태 변경
        val giftUser = giftUserRepository.findByGiftId(gift.id)
            ?: throw IllegalArgumentException("선물 수신 정보가 없습니다.")

        val now = OffsetDateTime.now()
        val updatedGiftUser = when (req.status) {
            GiftStatus.DELIVERY_DONE.code, GiftStatus.GRANTED.code -> {
                giftUser.copy(
                    status = req.status,
                    pubDate = now,
                    useDate = now
                )
            }
            else -> {
                giftUser.copy(
                    status = req.status,
                    requestDate = now
                )
            }
        }

        giftUserRepository.save(updatedGiftUser)

        // 5️⃣ 응답
        val response = repository.findByUid(req.giftUid)
            ?: throw IllegalArgumentException("선물 정보 조회 실패")

        return ResponseEntity.ok(ApiResult.success(response))
    }


    /**
     * 상품 카테고리 (관리자 모달용)
     */
    @GetMapping("/goods/categories")
    suspend fun getGoodsCategories(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<List<GoodsCategoryDto>>> {
        val list = giftishowGoodsRepository.findCategories().toList()
        return ResponseEntity.ok(ApiResult.success(list))
    }


    /**
     * 상품 종류 (관리자 모달용)
     */
    @GetMapping("/goods/types")
    suspend fun getGoodsTypes(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<List<GoodsTypeDto>>> {
        val list = giftishowGoodsRepository.findGoodsTypes().toList()
        return ResponseEntity.ok(ApiResult.success(list))
    }


    /**
     * 기프트쇼 상품 목록조회 (관리자 모달용)
     */
    @GetMapping("/goods")
    suspend fun getGoods(
        @RequestParam size: Int = 10,
        @RequestParam(required = false) lastId: Long?,
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam categorySeq: Int? = null,
        @RequestParam goodsTypeCd: String? = null,
        @RequestParam(required = false) brandCode: String? = null,
        @RequestParam(required = false) goodsCodeType: String? = null, // G: 기프티쇼, H: 기프트카드, D: 배송상품
        @RequestParam(required = false) lastBrandName: String? = null,
        @RequestParam(required = false) lastSalePrice: Int? = null,
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

        val lastItem = list.lastOrNull()
        val nextCursor = lastItem?.let { "${it.brandName}:${it.salePrice}:${it.id}" }

        return ResponseEntity.ok(
            ApiResult.success(
                ApiPageCursor(
                    items = list,
                    nextCursor = nextCursor,
                    perPage = size,
                )
            )
        )
    }


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

    /**
     * 바코드 이미지 조회 (관리자용)
     * - 바코드 asset uid로 직접 조회
     */
    @Operation(
        summary = "바코드 이미지 조회 (관리자용)",
        description = "관리자가 바코드 asset uid로 바코드 이미지를 조회합니다."
    )
    @GetMapping("/barcode/{barcodeUid}")
    suspend fun getBarcodeImage(
        @PathVariable barcodeUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<FileSystemResource> {
        // 1️⃣ Asset 조회
        val asset = assetService.findByUid(barcodeUid)
            ?: return ResponseEntity.notFound().build()

        // 2️⃣ 바코드 카테고리인지 확인
        if (!asset.filePath.startsWith("barcode/")) {
            return ResponseEntity.notFound().build()
        }

        // 3️⃣ 파일 경로 확인
        val path = assetService.resolveFilePath(asset.filePath + asset.storeFileName)
        if (!assetService.existsFile(path)) {
            return ResponseEntity.notFound().build()
        }

        // 4️⃣ 파일 반환
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
