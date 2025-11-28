package kr.jiasoft.hiteen.feature.gift_v2.app

import kr.jiasoft.hiteen.feature.gift_v2.dto.client.GiftishowApiResponse
import kr.jiasoft.hiteen.feature.gift_v2.dto.client.biz.GiftishowBizMoneyResult
import kr.jiasoft.hiteen.feature.gift_v2.dto.client.brand.GiftishowBrandDetailResult
import kr.jiasoft.hiteen.feature.gift_v2.dto.client.brand.GiftishowBrandListResult
import kr.jiasoft.hiteen.feature.gift_v2.dto.client.goods.GiftishowGoodsDetailResult
import kr.jiasoft.hiteen.feature.gift_v2.dto.client.goods.GiftishowGoodsListResult
import kr.jiasoft.hiteen.feature.gift_v2.dto.client.voucher.GiftishowVoucherDetailDto
import kr.jiasoft.hiteen.feature.gift_v2.dto.client.voucher.GiftishowVoucherDetailResult
import kr.jiasoft.hiteen.feature.gift_v2.dto.client.voucher.GiftishowVoucherSendRequest
import kr.jiasoft.hiteen.feature.gift_v2.dto.client.voucher.GiftishowVoucherSendResponseDto

interface GiftshowClient {
    suspend fun listGoods(start: String, size: String): GiftishowApiResponse<GiftishowGoodsListResult>
    suspend fun detailGoods(goodsCode: String): GiftishowApiResponse<GiftishowGoodsDetailResult>
    suspend fun listBrand(): GiftishowApiResponse<GiftishowBrandListResult>
    suspend fun detailBrand(brandCode: String): GiftishowApiResponse<GiftishowBrandDetailResult>
    suspend fun detailVoucher(trId: String): GiftishowApiResponse<GiftishowVoucherDetailDto>
    suspend fun issueVoucher(req: GiftishowVoucherSendRequest): GiftishowApiResponse<GiftishowVoucherSendResponseDto>
    suspend fun bizMoney(): GiftishowApiResponse<GiftishowBizMoneyResult>
}
