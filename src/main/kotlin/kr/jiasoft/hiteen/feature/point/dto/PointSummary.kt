package kr.jiasoft.hiteen.feature.point.dto

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import kr.jiasoft.hiteen.feature.point.domain.PointEntity

@Tag(name = "Point", description = "포인트 관련 API")
data class PointSummary (
@field:Schema(description = "보유 포인트", example = "83000")
    val total: Int,
    val history: List<PointEntity>,
)