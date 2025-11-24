package kr.jiasoft.hiteen.feature.gift.dto

import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.giftishow.dto.GiftishowGoodsResponse
import kr.jiasoft.hiteen.feature.user.dto.UserSummary
import java.time.OffsetDateTime

@Schema(description = "내 선물 조회 Response")
data class GiftUserResponse(

    @field:Schema(description = "상태 ( WAITING, SENT, USED, EXPIRED 등)")
    val status: String,

    @field:Schema(description = "선물 메시지 / 메모")
    val memo: String? = null,

    @field:Schema(description = "지급된 날짜")
    val issuedAt: OffsetDateTime,

    @field:Schema(description = "사용 가능 만료일")
    val expireAt: OffsetDateTime? = null,

    @field:Schema(description = "선물 사용자")
    val user: UserSummary,

    @field:Schema(description = "기프티쇼 상품 정보")
    val goods: GiftishowGoodsResponse?
)
