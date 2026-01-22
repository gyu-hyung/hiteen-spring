package kr.jiasoft.hiteen.admin.dto

import org.springframework.data.relational.core.mapping.Column

data class GoodsTypeDto(
    @Column("goods_type_cd")
    val goodsTypeCd: String?,
    @Column("goods_type_name")
    val goodsTypeName: String?
)
