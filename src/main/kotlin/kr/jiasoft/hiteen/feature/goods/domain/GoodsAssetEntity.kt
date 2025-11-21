package kr.jiasoft.hiteen.feature.goods.domain

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("goods_asset")
@Schema(description = "상품 이미지 파일 엔티티")
data class GoodsAssetEntity(

    @Id
    @field:Schema(description = "파일 PK")
    val id: Long = 0,

    @field:Schema(description = "상품 번호")
    @Column("goods_id")
    val goodsId: Long,

    @field:Schema(description = "파일 UUID")
    val uid: UUID,
)
