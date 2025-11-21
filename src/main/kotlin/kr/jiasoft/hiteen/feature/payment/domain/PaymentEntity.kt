package kr.jiasoft.hiteen.feature.payment.domain

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("payment")
@Schema(description = "결제 정보 엔티티")
data class PaymentEntity(

    @Id
    @field:Schema(description = "결제 PK")
    val id: Long = 0,

    @field:Schema(description = "결제 UID")
    val uid: UUID = UUID.randomUUID(),

    @field:Schema(description = "회원 ID (FK)")
    @Column("user_id")
    val userId: Long? = null,

    @field:Schema(description = "결제 유형 (Gift, Point 충전)")
    val type: PaymentType,

    @field:Schema(description = "주문자명")
    @Column("order_name")
    val orderName: String? = null,

    @field:Schema(description = "내부 상품 ID (FK)")
    @Column("goods_id")
    val goodsId: Long? = null,

    @field:Schema(description = "기프티쇼 상품코드 (FK)")
    @Column("goods_code")
    val goodsCode: String? = null,

    @field:Schema(description = "상품명")
    @Column("goods_name")
    val goodsName: String? = null,

    @field:Schema(description = "상품 판매가")
    @Column("goods_price")
    val goodsPrice: Int = 0,

    @field:Schema(description = "구매 수량")
    val amount: Int = 0,

    @field:Schema(description = "결제 금액")
    @Column("pay_price")
    val payPrice: Int = 0,

    @field:Schema(description = "포인트 지급량")
    val point: Int = 0,

    @field:Schema(description = "수수료")
    val tax: Int = 0,

    @field:Schema(description = "매출금액(정산용)")
    val sales: Int = 0,

    @field:Schema(description = "OS 유형 (ios/android/web)")
    @Column("os_type")
    val osType: String? = null,

    @field:Schema(description = "스토어 주문ID")
    @Column("order_id")
    val orderId: String? = null,

    @field:Schema(description = "패키지명 (Google, App Store 인증용)")
    @Column("package_name")
    val packageName: String? = null,

    @field:Schema(description = "스토어 구매 토큰")
    @Column("purchase_token")
    val purchaseToken: String? = null,

    @field:Schema(description = "참조 테이블 ID 목록(JSON/Text)")
    @Column("table_ids")
    val tableIds: String? = null,

    @field:Schema(description = "원본 응답 결과")
    val response: String? = null,

    @field:Schema(description = "메모")
    val memo: String? = null,

    @field:Schema(description = "결제 로그 (Webhook 포함)")
    val logs: String? = null,

    @field:Schema(description = "결제 상태")
    val status: PaymentStatus,

    @field:Schema(description = "등록일시")
    @Column("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @field:Schema(description = "수정일시")
    @Column("updated_at")
    val updatedAt: OffsetDateTime? = null,

    @field:Schema(description = "삭제일시")
    @Column("deleted_at")
    val deletedAt: OffsetDateTime? = null,
)
