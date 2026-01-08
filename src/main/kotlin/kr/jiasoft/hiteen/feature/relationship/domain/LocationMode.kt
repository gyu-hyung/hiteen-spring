package kr.jiasoft.hiteen.feature.relationship.domain

enum class LocationMode {
    PUBLIC, HIDDEN, RANDOM, NEARBY;

    companion object {
        fun from(value: String): LocationMode =
            LocationMode.entries.find { it.name.equals(value, ignoreCase = true) } ?: throw IllegalArgumentException("Invalid CodeGroup: $value")
    }
}
