package kr.jiasoft.hiteen.feature.point.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "포인트 충전 요청 DTO")
data class PointChargeRequest(
    @field:Schema(description = "결제 PG사 트랜잭션 ID")
    val paymentId: String,   // 결제 PG사 트랜잭션 ID
    @field:Schema(description = "결제 금액")
    val amount: Int          // 결제 금액 (1000, 3000, 5000, 10000)
)
