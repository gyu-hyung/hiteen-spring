package kr.jiasoft.hiteen.admin.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "상품 등록/수정 요청 DTO")
data class GoodsGiftishowCreateRequest(

    @field:Schema(description = "상품 ID (있으면 수정, 없으면 등록)")
    val id: Long? = null,

    val goodsNo: Int,
    val goodsCode: String,

    val goodsName: String? = null,
    val brandCode: String? = null,
    val brandName: String? = null,
    val content: String? = null,
    val contentAddDesc: String? = null,
    val searchKeyword: String? = null,
    val mdCode: String? = null,

    val category1Seq: Int? = null,
    val category1Name: String? = null,

    val goodsTypeCode: String? = null,
    val goodsTypeName: String? = null,
    val goodsTypeDetailName: String? = null,

    val goodsImgS: String? = null,
    val goodsImgB: String? = null,
    val goodsDescImgWeb: String? = null,
    val brandIconImg: String? = null,
    val mmsGoodsImg: String? = null,

    val salePrice: Int = 0,
    val realPrice: Int = 0,
    val discountRate: Double = 0.0,
    val discountPrice: Int = 0,

    val goodsComId: String? = null,
    val goodsComName: String? = null,

    val validPeriodType: String = "01",
    val limitDay: Int? = null,
    val validPeriodDay: String? = null,

    val goodsStateCode: String? = null,
    val status: Int = 0,
)
