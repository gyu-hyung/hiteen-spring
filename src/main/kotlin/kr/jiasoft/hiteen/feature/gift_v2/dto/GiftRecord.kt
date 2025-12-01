package kr.jiasoft.hiteen.feature.gift_v2.dto

import kr.jiasoft.hiteen.feature.gift.domain.GiftCategory
import kr.jiasoft.hiteen.feature.gift.domain.GiftType
import kr.jiasoft.hiteen.feature.user.dto.UserSummary
import java.time.OffsetDateTime

data class GiftRecord (
    val giftId: Long,
    val giftUid: String,
    val giftUserId: Long,
    val giftType: GiftType,
    val giftCategory: GiftCategory,
    val status: Int,
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

    )
    fun GiftRecord.toResponse(
        receiver: UserSummary
    ): GiftResponse {
        return GiftResponse(
            giftId = this.giftId,
            giftUid = this.giftUid,
            giftUserId = this.giftUserId,
            giftType = this.giftType,
            giftCategory = this.giftCategory,
            status = GiftStatus.from(this.status),
            userId = this.userId,

            memo = this.memo,

            couponNo = this.couponNo,
            couponImg = this.couponImg,
            receiveDate = this.receiveDate,
            requestDate = this.requestDate,
            pubDate = this.pubDate,
            useDate = this.useDate,
            pubExpiredDate = this.pubExpiredDate,
            useExpiredDate = this.useExpiredDate,

            goodsCode = this.goodsCode,
            goodsName = this.goodsName,

            gameId = this.gameId,
            seasonId = this.seasonId,
            seasonRank = this.seasonRank,

            point = this.point,

            deliveryName = this.deliveryName,
            deliveryPhone = this.deliveryPhone,
            deliveryAddress1 = this.deliveryAddress1,
            deliveryAddress2 = this.deliveryAddress2,

            receiver = receiver
        )
    }

