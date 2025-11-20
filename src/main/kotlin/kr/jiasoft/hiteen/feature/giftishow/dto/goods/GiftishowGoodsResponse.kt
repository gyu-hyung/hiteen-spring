package kr.jiasoft.hiteen.feature.giftishow.dto.goods

data class GiftishowGoodsResponse(
    val code: String,
    val message: String?,
    val result: GoodsResult?
) {
    data class Result(
        val listNum: Int,
        val goodsList: List<GoodsDto>
    )
}
