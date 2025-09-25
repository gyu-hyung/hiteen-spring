package kr.jiasoft.hiteen.feature.level.domain

enum class TierCode(
    val level: Int,
    val description: String
) {
    BRONZE_STAR(1, "별빛 브론즈"),
    BRONZE_MOON(1, "달빛 브론즈"),
    BRONZE_SUN(1, "태양 브론즈"),

    SILVER_STAR(2, "별빛 실버"),
    SILVER_MOON(2, "달빛 실버"),
    SILVER_SUN(2, "태양 실버"),

    GOLD_STAR(3, "별빛 골드"),
    GOLD_MOON(3, "달빛 골드"),
    GOLD_SUN(3, "태양 골드"),

    PLATINUM_STAR(4, "별빛 플래티넘"),
    PLATINUM_MOON(4, "달빛 플래티넘"),
    PLATINUM_SUN(4, "태양 플래티넘"),

    DIAMOND_STAR(5, "별빛 다이아몬드"),
    DIAMOND_MOON(5, "달빛 다이아몬드"),
    DIAMOND_SUN(5, "태양 다이아몬드"),

    MASTER_STAR(6, "별빛 마스터"),
    MASTER_MOON(6, "달빛 마스터"),
    MASTER_SUN(6, "태양 마스터"),

    GRANDMASTER_STAR(7, "별빛 그랜드마스터"),
    GRANDMASTER_MOON(7, "달빛 그랜드마스터"),
    GRANDMASTER_SUN(7, "태양 그랜드마스터"),

    CHALLENGER(8, "챌린저");

    companion object {
        fun fromCode(code: String): TierCode =
            entries.find { it.name == code }
                ?: throw IllegalArgumentException("Invalid tier code: $code")

        fun fromLevel(level: Int): List<TierCode> =
            entries.filter { it.level == level }
    }
}
