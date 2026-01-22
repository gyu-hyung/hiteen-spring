package kr.jiasoft.hiteen.feature.gift.app

import kr.jiasoft.hiteen.feature.gift.dto.GiftBuyRequest
import kr.jiasoft.hiteen.feature.gift.dto.GiftProvideRequest
import kr.jiasoft.hiteen.feature.gift.dto.GiftIssueRequest
import kr.jiasoft.hiteen.feature.gift.dto.GiftResponse
import kr.jiasoft.hiteen.feature.gift.dto.GiftUseRequest
import kr.jiasoft.hiteen.feature.giftishow.domain.GoodsGiftishowEntity
import java.util.UUID

interface GiftAppService {

    /** 선물 조회 */
    suspend fun findGift(receiverUserId: Long, giftUserId: Long) : GiftResponse?

    /** 사용자 -> 선물 구매 */
    suspend fun buyGift(userId: Long, userUid: UUID, req: GiftBuyRequest) : List<GiftResponse>

    /** 관리자 -> 사용자 선물 지급 */
    suspend fun createGift(userId: Long, req: GiftProvideRequest, sendPush: Boolean = true) : List<GiftResponse>

    /** 사용자 -> 선물 발급 */
    suspend fun issueGift(userId: Long, req: GiftIssueRequest) : GiftResponse

    /** 사용자 -> 선물 사용 */
    suspend fun useGift(userId: Long, req: GiftUseRequest) : GiftResponse

    /** 선물함 목록조회 */
    suspend fun listGift(userId: Long) : List<GiftResponse>

    suspend fun listGiftByCursor(userId: Long, size: Int, lastId: Long?) : List<GiftResponse>

    /** 기프트쇼 상품 목록조회 */
    suspend fun listGoods() : List<GoodsGiftishowEntity>

}