package kr.jiasoft.hiteen.feature.gift_v2.dto.client.brand

data class GiftishowBrandDto(
    val brandSeq: Int,
    val brandCode: String,
    val brandName: String,
    val brandBannerImg: String?,
    val brandIConImg: String?,
    val mmsThumImg: String?,
    val content: String?,
    val category1Seq: Int?,
    val category1Name: String?,
    val category2Seq: Int?,
    val category2Name: String?,
    val sort: Int?
)
