package kr.jiasoft.hiteen.feature.point.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "현재 보유 포인트")
data class PointBalanceResponse(
    @field:Schema(description = "현재 보유 포인트")
    val point: Int,
)

