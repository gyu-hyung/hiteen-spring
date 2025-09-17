package kr.jiasoft.hiteen.feature.code.domain

enum class CodeGroup {
    EMOJI,
    PIN_DESCRIPTION;

    companion object {
        fun from(value: String): CodeStatus =
            CodeStatus.entries.find { it.name.equals(value, ignoreCase = true) } ?: throw IllegalArgumentException("Invalid CodeGroup: $value")
    }
}