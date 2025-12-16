package kr.jiasoft.hiteen.feature.gift.app

import kr.jiasoft.hiteen.feature.gift.dto.GiftCreateRequest
import kr.jiasoft.hiteen.feature.gift.dto.GiftIssueRequest
import kr.jiasoft.hiteen.feature.gift.dto.GiftResponse
import kr.jiasoft.hiteen.feature.gift.dto.GiftUseRequest
import kr.jiasoft.hiteen.feature.giftishow.domain.GoodsGiftishowEntity

interface GiftAppService {

    /** 관리자 -> 사용자 선물 지급 */
    suspend fun createGift(userId: Long, req: kr.jiasoft.hiteen.feature.gift.dto.GiftCreateRequest) : kr.jiasoft.hiteen.feature.gift.dto.GiftResponse

    /** 사용자 -> 선물 발급 */
    suspend fun issueGift(userId: Long, req: kr.jiasoft.hiteen.feature.gift.dto.GiftIssueRequest) : kr.jiasoft.hiteen.feature.gift.dto.GiftResponse

    /** 사용자 -> 선물 사용 */
    suspend fun useGift(userId: Long, req: kr.jiasoft.hiteen.feature.gift.dto.GiftUseRequest) : kr.jiasoft.hiteen.feature.gift.dto.GiftResponse

    /** 선물함 목록조회 */
    suspend fun listGift(userId: Long) : List<kr.jiasoft.hiteen.feature.gift.dto.GiftResponse>

    /** 기프트쇼 상품 목록조회 */
    suspend fun listGoods() : List<GoodsGiftishowEntity>

}