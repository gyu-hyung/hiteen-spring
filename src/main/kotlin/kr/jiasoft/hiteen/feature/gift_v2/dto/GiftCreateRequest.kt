package kr.jiasoft.hiteen.feature.gift_v2.dto

import kr.jiasoft.hiteen.feature.gift.domain.GiftCategory
import kr.jiasoft.hiteen.feature.gift.domain.GiftType
import java.util.UUID

data class GiftCreateRequest (
    val giftType: GiftType,
    val giftCategory: GiftCategory,
    val receiveUserUid: UUID,

    val memo: String? = null,

    val goodsCode: String? = null,
    val gameId: Long? = null,
    val seasonId: Long? = null,
    val seasonRank: Int? = null,

    val point: Int? = null,

    val deliveryName: String? = null,
    val deliveryPhone: String? = null,
    val deliveryAddress1: String? = null,
    val deliveryAddress2: String? = null,
) {
//    fun toGiftEntity(): GiftEntity {
//        return GiftEntity(
//            type = giftType,
//            category = giftCategory,
//            userId = receiveUserId,
//            memo = memo,
//        )
//    }
}