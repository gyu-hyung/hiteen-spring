package kr.jiasoft.hiteen.admin.app

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminGiftResponse
import kr.jiasoft.hiteen.admin.dto.GoodsCategoryDto
import kr.jiasoft.hiteen.admin.dto.GoodsTypeDto
import kr.jiasoft.hiteen.admin.infra.AdminGiftRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiPageCursor
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.gift.app.GiftAppService
import kr.jiasoft.hiteen.feature.gift.app.GiftshowClient
import kr.jiasoft.hiteen.feature.gift.domain.GiftCategory
import kr.jiasoft.hiteen.feature.gift.domain.GiftType
import kr.jiasoft.hiteen.feature.gift.dto.GiftProvideRequest
import kr.jiasoft.hiteen.feature.gift.dto.GiftResponse
import kr.jiasoft.hiteen.feature.gift.dto.client.GiftishowApiResponse
import kr.jiasoft.hiteen.feature.gift.dto.client.biz.GiftishowBizMoneyResult
import kr.jiasoft.hiteen.feature.giftishow.domain.GoodsGiftishowEntity
import kr.jiasoft.hiteen.feature.giftishow.infra.GiftishowGoodsRepository
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/admin/gift")
@PreAuthorize("hasRole('ADMIN')")
class AdminGiftController(
    private val repository: AdminGiftRepository,
    private val giftAppService: GiftAppService,
    private val giftishowGoodsRepository: GiftishowGoodsRepository,
    private val giftshowClient: GiftshowClient,
) {

    /**
     * 관리자 -> 사용자 선물 생성(지급)
     * 기존: POST /api/gift/admin/create
     */
    @PostMapping("/create")
    suspend fun createGift(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        request: GiftProvideRequest,
    ): ResponseEntity<ApiResult<GiftResponse>> {
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
     * 기프트쇼 발송 취소 (trId 기준)
     * - 실제로 쿠폰이 발송된 상태(SENT)에서만 의미가 있음
     */
    @PostMapping("/voucher/cancel")
    suspend fun cancelVoucher(
        @RequestParam trId: String,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<GiftishowApiResponse<String>>> {
        return ResponseEntity.ok(ApiResult.success(giftshowClient.cancelVoucher(trId)))
    }


    /**
     * 상품 카테고리 (관리자 모달용)3
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
        val list = giftishowGoodsRepository.findGoodsTypes()?.toList()
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
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPageCursor<GoodsGiftishowEntity>>> {

        val list = giftishowGoodsRepository.listByCursorId(
            size = size,
            lastId = lastId,
            search = search,
            searchType = searchType,
            categorySeq = categorySeq,
            goodsTypeCd = goodsTypeCd,
        ).toList()

        val nextCursor = list.lastOrNull()?.id?.toString()

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
}
