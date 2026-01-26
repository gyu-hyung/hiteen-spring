package kr.jiasoft.hiteen.admin.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "상품 상태 일괄 변경 요청")
data class AdminGoodsBulkStatusRequest(
    @field:Schema(description = "변경 대상 상품 ID 목록")
    val ids: List<Long>,

    @field:Schema(description = "변경할 상태 (1=ACTIVE, 0=INACTIVE)")
    val status: Int,
)

