package kr.jiasoft.hiteen.feature.gift.dto

import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.gift.domain.GiftCategory
import kr.jiasoft.hiteen.feature.gift.domain.GiftType
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "선물 응답 DTO")
data class GiftResponse(

    @field:Schema(description = "선물 고유 ID")
    val id: Long,

    @field:Schema(description = "선물 UUID")
    val uid: UUID,

    @field:Schema(description = "타입")
    val type: GiftType,

    @field:Schema(description = "카테고리")
    val category: GiftCategory,

    @field:Schema(description = "회원 ID")
    val userId: Long,

    @field:Schema(description = "결제 ID")
    val payId: Long?,

    @field:Schema(description = "메모")
    val memo: String?,

    @field:Schema(description = "받는 회원 목록")
    val users: List<Long>,

    @field:Schema(description = "생성일시")
    val createdAt: OffsetDateTime?,
)
