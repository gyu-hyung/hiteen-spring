package kr.jiasoft.hiteen.feature.giftishow.app
import kr.jiasoft.hiteen.feature.giftishow.dto.GiftishowGoodsResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/giftishow")
class GiftishowController(
    private val service: GiftishowService
) {

    @GetMapping("/goods")
    suspend fun getGoodsList(): ResponseEntity<List<GiftishowGoodsResponse>> =
        ResponseEntity.ok(service.getGoodsList())

    @GetMapping("/goods/{goodsCode}")
    suspend fun getGoods(@PathVariable goodsCode: String): ResponseEntity<GiftishowGoodsResponse> =
        ResponseEntity.ok(service.getGoods(goodsCode))
}