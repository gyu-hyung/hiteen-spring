package kr.jiasoft.hiteen.feature.ad.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "광고 보상 요청 DTO")
data class AdRewardRequest(

    @field:Schema(description = "Admob SDK에서 전달한 트랜잭션")
    val transactionId: String,
    @field:Schema(description = "보상 금액")
    val rewardAmount: Int = 100,
    @field:Schema(description = "Admob SDK에서 전달한 원본 데이터")
    val rawData: String? = null
)
