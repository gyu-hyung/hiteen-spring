package kr.jiasoft.hiteen.feature.gift.dto

import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.gift.domain.GiftCategory
import kr.jiasoft.hiteen.feature.gift.domain.GiftType
import java.util.UUID

@Schema(description = "선물 생성 요청")
data class GiftProvideRequest (
    @field:Schema(description = "구분 (Point, Voucher, Delivery, GiftCard)")
    val giftType: GiftType,
    @field:Schema(description = "카테고리 (Join, Challenge, Admin, Event, Shop)")
    val giftCategory: GiftCategory,
    @field:Schema(description = "수신자 uid")
    val receiveUserUid: UUID,
    @field:Schema(description = "메모")
    val memo: String? = null,

    @field:Schema(description = "상품코드 - Voucher")
    val goodsCode: String? = null,
    @field:Schema(description = "게임 ID - Challenge")
    val gameId: Long? = null,
    @field:Schema(description = "시즌 ID - Challenge")
    val seasonId: Long? = null,
    @field:Schema(description = "시즌 등수 - Challenge")
    val seasonRank: Int? = null,

    @field:Schema(description = "부여 포인트 - Point")
    val point: Int? = null,

    @field:Schema(description = "배송 수신자명 - Delivery")
    val deliveryName: String? = null,
    @field:Schema(description = "배송 수신자 연락처 - Delivery")
    val deliveryPhone: String? = null,
    @field:Schema(description = "배송 수신자 주소1 - Delivery")
    val deliveryAddress1: String? = null,
    @field:Schema(description = "배송 수신자 주소2 - Delivery")
    val deliveryAddress2: String? = null,
)