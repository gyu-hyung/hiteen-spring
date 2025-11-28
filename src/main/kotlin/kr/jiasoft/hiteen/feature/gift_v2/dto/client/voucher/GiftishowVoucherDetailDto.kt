package kr.jiasoft.hiteen.feature.gift_v2.dto.client.voucher

data class GiftishowVoucherDetailDto(
    val goodsCd: String,
    val pinStatusCd: String,
    val goodsNm: String,
    val sellPriceAmt: String,
    val remailAmt: String?,
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
