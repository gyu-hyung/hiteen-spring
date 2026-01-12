package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import kr.jiasoft.hiteen.feature.gift.domain.GiftCategory
import kr.jiasoft.hiteen.feature.gift.domain.GiftType
import java.time.OffsetDateTime
import java.util.UUID

data class AdminGiftResponse (
    val giftId: Long,
    val giftUid: UUID,
    val giftUserId: Long,
    val giftCategory: GiftCategory,
    val giftType: GiftType,
    val memo: String,
    val giverNickname: String,
    val giverUserId: Long,
    val receiverNickname: String,
    val receiverUserId: Long,
    val status: String,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val receiveDate: OffsetDateTime,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime,
    val couponNo: String? = null,
    val couponImg: String? = null,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val requestDate: OffsetDateTime? = null,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val pubDate: OffsetDateTime? = null,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val useDate: OffsetDateTime? = null,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val pubExpiredDate: OffsetDateTime? = null,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val useExpiredDate: OffsetDateTime? = null,
    val goodsCode: String? = null,
    val goodsName: String? = null,
    val gameId: Long? = null,
    val seasonId: Long? = null,
    val seasonRank: Long? = null,
    val point: Long? = null,
    val deliveryName: String? = null,
    val deliveryPhone: String? = null,
    val deliveryAddress1: String? = null,
    val deliveryAddress2: String? = null
)