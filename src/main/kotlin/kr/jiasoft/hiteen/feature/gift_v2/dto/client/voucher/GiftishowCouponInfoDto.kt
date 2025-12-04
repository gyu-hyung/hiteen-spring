package kr.jiasoft.hiteen.feature.gift_v2.dto.client.voucher

data class GiftishowCouponInfoDto(
    val goodsCd: String,
    val pinStatusCd: String,
    val goodsNm: String,
    val sellPriceAmt: String,
    val remainAmt: String?,      // JSON: remainAmt (오타 주의)
    val senderTelNo: String?,
    val cnsmPriceAmt: String?,
    val sendRstCd: String?,
    val pinStatusNm: String?,
    val mmsBrandThumImg: String?,
    val brandNm: String?,
    val sendRstMsg: String?,
    val correcDtm: String?,
    val recverTelNo: String?,
    val validPrdEndDt: String?,
    val sendBasicCd: String?,
    val sendStatusCd: String?
)

