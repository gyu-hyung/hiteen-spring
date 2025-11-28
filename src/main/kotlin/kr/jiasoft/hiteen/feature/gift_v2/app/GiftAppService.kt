package kr.jiasoft.hiteen.feature.gift_v2.app

import kr.jiasoft.hiteen.feature.gift_v2.dto.GiftCreateRequest
import kr.jiasoft.hiteen.feature.gift_v2.dto.GiftIssueRequest

interface GiftAppService {

    /** 관리자 -> 사용자 선물 지급 */
    suspend fun createGift(userId: Long, req: GiftCreateRequest)

    /** 사용자 -> 선물 발급 */
    suspend fun issueGift(userId: Long, req: GiftIssueRequest)

    /** 사용자 -> 선물 사용 */
    suspend fun useGift()

    /** 선물함 목록조회 */
    suspend fun listGift()

    /** 기프트쇼 상품 목록조회 */
    suspend fun listGoods()

}