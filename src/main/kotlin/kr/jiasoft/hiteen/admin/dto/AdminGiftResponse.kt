package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
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
    val status: Int,

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
    val goodsImgS: String? = null,
    val goodsImgB: String? = null,
    val brandIconImg: String? = null,
    val gameId: Long? = null,
    val seasonId: Long? = null,
    val seasonRank: Long? = null,
    val point: Long? = null,
    val deliveryName: String? = null,
    val deliveryPhone: String? = null,
    val deliveryAddress1: String? = null,
    val deliveryAddress2: String? = null
) {
    @get:Schema(description = "상태 명칭")
    val statusName: String
        get() = when (giftType) {
            GiftType.Voucher -> when (status) {
                0 -> "대기"
                1 -> "발행"
                2 -> "사용 완료"
                3 -> "기간 만료"
                -1 -> "취소"
                else -> "알 수 없음($status)"
            }
            GiftType.Delivery -> when (status) {
                0 -> "대기"
                4 -> "배송 요청"
                5 -> "배송 완료"
                -1 -> "취소"
                else -> "알 수 없음($status)"
            }
            GiftType.GiftCard -> when (status) {
                0 -> "대기"
                6 -> "지급 요청"
                7 -> "지급 완료"
                -1 -> "취소"
                else -> "알 수 없음($status)"
            }
            else -> when (status) {
                -1 -> "취소"
                else -> "알 수 없음($status)"
            }
        }
}
