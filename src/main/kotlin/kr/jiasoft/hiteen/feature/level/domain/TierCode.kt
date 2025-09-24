package kr.jiasoft.hiteen.feature.level.domain

enum class TierCode(val description: String) {
    BRONZE_STAR("별빛 브론즈"),
    BRONZE_MOON("달빛 브론즈"),
    BRONZE_SUN("태양 브론즈"),

    SILVER_STAR("별빛 실버"),
    SILVER_MOON("달빛 실버"),
    SILVER_SUN("태양 실버"),

    GOLD_STAR("별빛 골드"),
    GOLD_MOON("달빛 골드"),
    GOLD_SUN("태양 골드"),

    PLATINUM_STAR("별빛 플래티넘"),
    PLATINUM_MOON("달빛 플래티넘"),
    PLATINUM_SUN("태양 플래티넘"),

    DIAMOND_STAR("별빛 다이아몬드"),
    DIAMOND_MOON("달빛 다이아몬드"),
    DIAMOND_SUN("태양 다이아몬드"),

    MASTER_STAR("별빛 마스터"),
    MASTER_MOON("달빛 마스터"),
    MASTER_SUN("태양 마스터"),

    GRANDMASTER_STAR("별빛 그랜드마스터"),
    GRANDMASTER_MOON("달빛 그랜드마스터"),
    GRANDMASTER_SUN("태양 그랜드마스터"),

    CHALLENGER("챌린저");

    companion object {
        fun fromCode(code: String): TierCode =
            entries.find { it.name == code }
                ?: throw IllegalArgumentException("Invalid tier code: $code")
    }
}
