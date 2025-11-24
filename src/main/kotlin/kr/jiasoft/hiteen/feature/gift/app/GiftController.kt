package kr.jiasoft.hiteen.feature.gift.app

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.gift.dto.GiftRequest
import kr.jiasoft.hiteen.feature.gift.dto.GiftResponse
import kr.jiasoft.hiteen.feature.gift.dto.GiftUserResponse
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/gift")
@Tag(name = "Gift API", description = "선물 생성 및 조회 API")
class GiftController(
    private val service: GiftService
) {

//    @PostMapping
//    suspend fun createGift(
//        @AuthenticationPrincipal(expression = "user") user: UserEntity,
//        @Parameter(description = "선물 생성 요청 DTO") request: GiftRequest)
//    : ResponseEntity<ApiResult<GiftResponse>> =
//        ResponseEntity.ok(ApiResult.success(service.createGift(request)))
//
//    @GetMapping
//    suspend fun getGifts(): ResponseEntity<ApiResult<List<GiftResponse>>> =
//        ResponseEntity.ok(ApiResult.success(service.getGifts()))

    @GetMapping("/my")
    suspend fun getMyGifts(@AuthenticationPrincipal(expression = "user") user: UserEntity)
        : ResponseEntity<ApiResult<List<GiftUserResponse>>> =
        ResponseEntity.ok(ApiResult.success(service.getMyGifts(user.id)))

}
