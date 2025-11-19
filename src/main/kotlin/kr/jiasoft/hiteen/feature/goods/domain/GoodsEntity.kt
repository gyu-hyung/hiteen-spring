package kr.jiasoft.hiteen.feature.goods.domain

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID


@Table("goods")
@Schema(description = "상품 엔티티 (포인트 충전 상품 포함)")
data class GoodsEntity(

    @Id
    @field:Schema(description = "상품 PK")
    val id: Long = 0,

    @field:Schema(description = "상품 UID")
    val uid: UUID = UUID.randomUUID(),

    @field:Schema(description = "상품 타입 (Point, Voucher, Delivery, Etc)")
    val type: GoodsType,

    @field:Schema(description = "카테고리명 (ex: Health, Gift, Admin)")
    val category: String? = null,

    @field:Schema(description = "상품명")
    val name: String? = null,

    @field:Schema(description = "상품 설명")
    val description: String? = null,

    @field:Schema(description = "판매 가격")
    val salePrice: Int = 0,

    @field:Schema(description = "할인 금액")
    val discount: Int = 0,

    @field:Schema(description = "최종 구매 가격")
    val price: Int = 0,

    @field:Schema(description = "구매 시 지급 포인트")
    val point: Int = 0,

    @field:Schema(description = "추가 지급 포인트")
    val bonusPoint: Int = 0,

    @field:Schema(description = "노출 여부(hidden/visible)")
    val status: GoodsStatus = GoodsStatus.hidden,

    @field:Schema(description = "메모")
    val memo: String? = null,

    @field:Schema(description = "생성일시")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @field:Schema(description = "수정일시")
    val updatedAt: OffsetDateTime? = null,

    @field:Schema(description = "삭제일시")
    val deletedAt: OffsetDateTime? = null
)
