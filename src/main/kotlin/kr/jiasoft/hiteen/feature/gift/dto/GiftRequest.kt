package kr.jiasoft.hiteen.feature.gift.dto


import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.gift.domain.GiftCategory
import kr.jiasoft.hiteen.feature.gift.domain.GiftType


@Schema(description = "선물 요청 DTO")
data class GiftRequest(

    @field:Schema(description = "선물 타입 (Point, Voucher, Delivery, Etc)")
    val type: GiftType,

    @field:Schema(description = "카테고리")
    val category: GiftCategory,

    @field:Schema(description = "회원 ID")
    val userId: Long,

    @field:Schema(description = "결제 ID(Optional)")
    val payId: Long? = null,

    @field:Schema(description = "메모(Optional)")
    val memo: String? = null,

    @field:Schema(description = "받는 사용자 ID 리스트")
    val users: List<Long> = emptyList()
)
