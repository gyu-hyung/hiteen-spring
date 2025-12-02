package kr.jiasoft.hiteen.feature.gift_v2.dto

import kr.jiasoft.hiteen.feature.gift.domain.GiftCategory
import kr.jiasoft.hiteen.feature.gift.domain.GiftType
import kr.jiasoft.hiteen.feature.giftishow.domain.GoodsGiftishowEntity
import kr.jiasoft.hiteen.feature.user.dto.UserSummary
import java.time.OffsetDateTime

data class GiftResponse (
    val giftId: Long,
    val giftUid: String,
    val giftUserId: Long,
    val giftType: GiftType,
    val giftCategory: GiftCategory,
    val status: GiftStatus,
    val userId: Long,

    val memo: String,

    val couponNo: String? = null,
    val couponImg: String? = null,
    val receiveDate: OffsetDateTime? = null,//선물수신일자
    val requestDate: OffsetDateTime? = null,//발송요청일자
    val pubDate: OffsetDateTime? = null,//발송일자
    val useDate: OffsetDateTime? = null,//사용일자
    val pubExpiredDate: OffsetDateTime? = null,//발송만료일자
    val useExpiredDate: OffsetDateTime? = null,//사용만료일자

    val goodsCode: String? = null,

    /* 추가 */
    val goodsName: String? = null,

    val gameId: Long? = null,

    val seasonId: Long? = null,
    val seasonRank: Int? = null,

    val point: Int? = null,

    val deliveryName: String? = null,
    val deliveryPhone: String? = null,
    val deliveryAddress1: String? = null,
    val deliveryAddress2: String? = null,

    val receiver: UserSummary,
    val goods: GoodsGiftishowEntity? = null,
)
