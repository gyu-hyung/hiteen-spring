package kr.jiasoft.hiteen.feature.gift.app

import kr.jiasoft.hiteen.feature.gift.dto.client.GiftishowApiResponse
import kr.jiasoft.hiteen.feature.gift.dto.client.biz.GiftishowBizMoneyResult
import kr.jiasoft.hiteen.feature.gift.dto.client.brand.GiftishowBrandDetailResult
import kr.jiasoft.hiteen.feature.gift.dto.client.brand.GiftishowBrandListResult
import kr.jiasoft.hiteen.feature.gift.dto.client.goods.GiftishowGoodsDetailResult
import kr.jiasoft.hiteen.feature.gift.dto.client.goods.GiftishowGoodsListResult
import kr.jiasoft.hiteen.feature.gift.dto.client.voucher.GiftishowInnerResponse
import kr.jiasoft.hiteen.feature.gift.dto.client.voucher.GiftishowVoucherSendRequest
import kr.jiasoft.hiteen.feature.gift.dto.client.voucher.GiftishowVoucherSendResponseDto


interface GiftshowClient {
    suspend fun listGoods(start: String, size: String): GiftishowApiResponse<GiftishowGoodsListResult>
    suspend fun detailGoods(goodsCode: String): GiftishowApiResponse<GiftishowGoodsDetailResult>
    suspend fun listBrand(): GiftishowApiResponse<GiftishowBrandListResult>
    suspend fun detailBrand(brandCode: String): GiftishowApiResponse<GiftishowBrandDetailResult>
//    suspend fun detailVoucher(trId: String): GiftishowApiResponse<List<GiftishowVoucherDetailWrapperDto>>
    suspend fun detailVoucher(trId: String): Map<String, Any?>
    suspend fun issueVoucher(req: GiftishowVoucherSendRequest): GiftishowApiResponse<GiftishowInnerResponse<GiftishowVoucherSendResponseDto>>
    suspend fun bizMoney(): GiftishowApiResponse<GiftishowBizMoneyResult>
}
