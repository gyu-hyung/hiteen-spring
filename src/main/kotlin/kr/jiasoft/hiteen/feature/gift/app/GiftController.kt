package kr.jiasoft.hiteen.feature.gift.app

import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.GoodsCategoryDto
import kr.jiasoft.hiteen.admin.dto.GoodsTypeDto
import kr.jiasoft.hiteen.common.dto.ApiPageCursor
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.gift.dto.GiftBuyRequest
import kr.jiasoft.hiteen.feature.gift.dto.GiftProvideRequest
import kr.jiasoft.hiteen.feature.gift.dto.GiftIssueRequest
import kr.jiasoft.hiteen.feature.gift.dto.GiftResponse
import kr.jiasoft.hiteen.feature.gift.dto.GiftUseRequest
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


@RestController
@RequestMapping("/api/gift")
@Tag(name = "Gift API", description = "선물 생성 및 조회 API")
class GiftController (
    private val giftAppService: GiftAppService,
    private val giftishowGoodsRepository: GiftishowGoodsRepository,
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
    ): ResponseEntity<ApiResult<GiftResponse>> {
        return ResponseEntity.ok(ApiResult.success(giftAppService.buyGift(user.id, user.uid, request)))
    }


    /**
     * 관리자 -> 사용자 선물 지급
     * */
    @PostMapping("/admin/create")
    @PreAuthorize("hasRole('ADMIN')")
    suspend fun createGift(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        request: GiftProvideRequest
    ): ResponseEntity<ApiResult<GiftResponse>> {
        return ResponseEntity.ok(ApiResult.success(giftAppService.createGift(user.id, request)))
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
     * 선물함 목록조회
     * */
    @GetMapping("/myGiftList")
    suspend fun myGiftList(
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ) : ResponseEntity<ApiResult<List<GiftResponse>>> {
        return ResponseEntity.ok(ApiResult.success(giftAppService.listGift(user.id)))
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
    suspend fun getGoods(
        @RequestParam size: Int = 10,
        @RequestParam(required = false) lastId: Long?,   // ⭐ id 커서
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
//            status = "ACTIVE",
            categorySeq = categorySeq,
            goodsTypeCd = goodsTypeCd,
        ).toList()

        // 다음 커서 (마지막 id)
        val nextCursor = list.lastOrNull()?.id?.toString()

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




}

