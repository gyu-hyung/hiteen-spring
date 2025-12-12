package kr.jiasoft.hiteen.feature.gift_v2.dto

import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.gift.domain.GiftType
import java.util.UUID

@Schema(description = "선물 발급 요청")
data class GiftIssueRequest (
    @field:Schema(description = "구분 (Point, Voucher, Delivery, GiftCard)")
    val giftType: GiftType,
//    val giftCategory: GiftCategory,
    @field:Schema(description = "선물 uid")
    val giftUid: UUID,
    @field:Schema(description = "기프티쇼 수신 연락처 - Voucher")
    val phone: String? = null,
    @field:Schema(description = "예약 여부 - Voucher")
    val revInfoYn: String = "N",
    @field:Schema(description = "예약 여부 - Voucher")
    val revInfoDate: String? = null,//yyyyMMdd
    @field:Schema(description = "예약 시간 - Voucher")
    val revInfoTime: String? = null,//HHmm
//    @field:Schema(description = " - Voucher")
//    val templateId: String? = null,
//    @field:Schema(description = " - Voucher")
//    val bannerId: String? = null,
//    val userId: String,
    @field:Schema(description = "발급방식 (Y:핀번호수신, N:MMS, I:이미지수신) - Voucher")
    val gubun: String = "I",
    @field:Schema(description = "배송 수신자 - Delivery")
    val deliveryName: String? = null,
    @field:Schema(description = "배송 연락처 - Delivery")
    val deliveryPhone: String? = null,
    @field:Schema(description = "배송 주소1 - Delivery")
    val deliveryAddress1: String? = null,
    @field:Schema(description = "배송 주소2 - Delivery")
    val deliveryAddress2: String? = null,
)