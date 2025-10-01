package kr.jiasoft.hiteen.feature.play.domain

enum class League(val displayName: String, val tiers: IntRange) {
    BRONZE("브론즈", 1..3),
    SILVER("실버", 4..5),
    GOLD("골드", 6..7),
    PLATINUM("플래티넘", 8..9),
    DIAMOND("다이아몬드", 10..11),
    MASTER("마스터", 12..13),
    GRANDMASTER("그랜드마스터", 14..15),
    CHALLENGER("챌린저", 16..Int.MAX_VALUE);

    companion object {
        fun fromTier(tierId: Int): League =
            League.entries.first { tierId in it.tiers }
    }
}
