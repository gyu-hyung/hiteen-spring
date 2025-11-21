package kr.jiasoft.hiteen.feature.goods.domain

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("goods")
@Schema(description = "상품 정보 엔티티")
data class GoodsEntity(

    @Id
    @field:Schema(description = "상품 ID")
    val id: Long = 0,

    @field:Schema(description = "상품 UID")
    val uid: String = UUID.randomUUID().toString(),

    @field:Schema(description = "상품 유형 (Point, Voucher, Delivery, Etc)")
    val type: String = "Etc",

    @field:Schema(description = "카테고리")
    val category: String? = null,

    @field:Schema(description = "상품명")
    val name: String? = null,

    @field:Schema(description = "상품 설명")
    val description: String? = null,

    @field:Schema(description = "브랜드명")
    val brand: String? = null,

    @field:Schema(description = "판매처명")
    val seller: String? = null,

    @field:Schema(description = "교환처명")
    val exchange: String? = null,

    @field:Schema(description = "유효기간(일)")
    @Column("limit_days")
    val limitDays: Int = 0,

    @field:Schema(description = "판매가격")
    @Column("sale_price")
    val salePrice: Int = 0,

    @field:Schema(description = "할인 금액")
    val discount: Int = 0,

    @field:Schema(description = "구매가격 (최종 지불금)")
    val price: Int = 0,

    @field:Schema(description = "적립 포인트")
    val point: Int = 0,

    @field:Schema(description = "추가 보너스 포인트")
    @Column("bonus_point")
    val bonusPoint: Int = 0,

    @field:Schema(description = "메모")
    val memo: String? = null,

    @field:Schema(description = "상태 (ex: ACTIVE, INACTIVE, DISABLED)")
    val status: String? = null,

    @field:Schema(description = "생성일시")
    @Column("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @field:Schema(description = "수정일시")
    @Column("updated_at")
    val updatedAt: OffsetDateTime? = null,

    @field:Schema(description = "삭제일시")
    @Column("deleted_at")
    val deletedAt: OffsetDateTime? = null,
)
