package kr.jiasoft.hiteen.feature.gift.domain

object GiftMessageFormatter {

    fun challengeMemo(gameName: String?, seasonName: String?, seasonRank: Int?): String {
        return "`${gameName ?: "챌린지"} ${seasonRank ?: "-"}위! 🎉 ${seasonName ?: ""}`"
    }

    fun challengeGoodsName(baseGoodsName: String?): String {
        return baseGoodsName ?: "챌린지 리워드"
    }

    fun challengeMmsMsg(goodsName: String?): String {
        return "🔥 축하합니다! '$goodsName' 리워드가 지급되었어요!🎁 지금 확인해보세요!"
    }
}
