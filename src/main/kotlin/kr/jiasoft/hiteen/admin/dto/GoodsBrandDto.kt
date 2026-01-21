package kr.jiasoft.hiteen.admin.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "상품 브랜드(필터용)")
data class GoodsBrandDto(
    @field:Schema(description = "브랜드 코드")
    val brandCode: String?,

    @field:Schema(description = "브랜드명")
    val brandName: String?,
)

