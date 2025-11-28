package kr.jiasoft.hiteen.feature.gift_v2.dto

import kr.jiasoft.hiteen.feature.gift.domain.GiftCategory
import kr.jiasoft.hiteen.feature.gift.domain.GiftType
import java.util.UUID

data class GiftIssueRequest (
    val giftType: GiftType,
    val giftCategory: GiftCategory,
    val giftUid: UUID,
    val phone: String? = null,
    val revInfoYn: String = "N",
    val revInfoDate: String? = null,//yyyyMMdd
    val revInfoTime: String? = null,//HHmm
    val templateId: String? = null,
    val bannerId: String? = null,
//    val userId: String,
    val gubun: String = "I",
    val deliveryName: String? = null,
    val deliveryPhone: String? = null,
    val deliveryAddress1: String? = null,
    val deliveryAddress2: String? = null,
)