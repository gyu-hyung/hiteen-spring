package kr.jiasoft.hiteen.admin.dto

import org.springframework.data.relational.core.mapping.Column

data class GoodsBrandDto(
    @Column("brand_code")
    val brandCode: String,
    @Column("brand_name")
    val brandName: String
)

