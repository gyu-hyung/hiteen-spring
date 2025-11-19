package kr.jiasoft.hiteen.feature.payment.domain

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("payment")
@Schema(description = "결제 엔티티")
data class PaymentEntity(

    @Id
    @field:Schema(description = "결제 PK")
    val id: Long = 0,

    @field:Schema(description = "결제 UID")
    val uid: UUID = UUID.randomUUID(),

    @Column("user_id")
    @field:Schema(description = "회원 번호")
    val userId: Long? = null,

    @field:Schema(description = "결제 유형 (Gift, Point 충전)")
    val type: PaymentType,

    @Column("order_name")
    @field:Schema(description = "주문 이름")
    val orderName: String? = null,

    @Column("goods_id")
    @field:Schema(description = "상품 ID")
    val goodsId: Long? = null,

    @field:Schema(description = "상품 코드")
    val goodsCode: String? = null,

    @field:Schema(description = "상품명")
    val goodsName: String? = null,

    @field:Schema(description = "상품 원가")
    val goodsPrice: Int = 0,

    @field:Schema(description = "구매 수량")
    val amount: Int = 0,

    @field:Schema(description = "최종 결제 금액")
    val payPrice: Int = 0,

    @field:Schema(description = "적립 포인트")
    val point: Int = 0,

    @field:Schema(description = "세금")
    val tax: Int = 0,

    @field:Schema(description = "매출 금액")
    val sales: Int = 0,

    @field:Schema(description = "결제 상태")
    val status: PaymentStatus,

    @field:Schema(description = "단말기 결제 토큰")
    val purchaseToken: String? = null,

    @field:Schema(description = "메모")
    val memo: String? = null,

    @Column("created_at")
    @field:Schema(description = "결제 생성일시")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column("updated_at")
    @field:Schema(description = "수정일시")
    val updatedAt: OffsetDateTime? = null,

    @Column("deleted_at")
    @field:Schema(description = "삭제일시")
    val deletedAt: OffsetDateTime? = null
)