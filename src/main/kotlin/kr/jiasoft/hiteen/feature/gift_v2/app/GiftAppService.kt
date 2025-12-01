package kr.jiasoft.hiteen.feature.gift_v2.app

import kr.jiasoft.hiteen.feature.gift_v2.dto.GiftCreateRequest
import kr.jiasoft.hiteen.feature.gift_v2.dto.GiftIssueRequest
import kr.jiasoft.hiteen.feature.gift_v2.dto.GiftResponse
import kr.jiasoft.hiteen.feature.gift_v2.dto.GiftUseRequest
import kr.jiasoft.hiteen.feature.giftishow.domain.GoodsGiftishowEntity

interface GiftAppService {

    /** 관리자 -> 사용자 선물 지급 */
    suspend fun createGift(userId: Long, req: GiftCreateRequest) : GiftResponse

    /** 사용자 -> 선물 발급 */
    suspend fun issueGift(userId: Long, req: GiftIssueRequest) : GiftResponse

    /** 사용자 -> 선물 사용 */
    suspend fun useGift(userId: Long, req: GiftUseRequest) : GiftResponse

    /** 선물함 목록조회 */
    suspend fun listGift(userId: Long) : List<GiftResponse>

    /** 기프트쇼 상품 목록조회 */
    suspend fun listGoods() : List<GoodsGiftishowEntity>

}