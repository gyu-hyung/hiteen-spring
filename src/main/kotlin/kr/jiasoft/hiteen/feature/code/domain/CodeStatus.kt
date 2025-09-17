package kr.jiasoft.hiteen.feature.code.domain

enum class CodeStatus {
    ACTIVE,
    INACTIVE;

    companion object {
        fun from(value: String?): CodeStatus =
            CodeStatus.entries.find { it.name.equals(value, ignoreCase = true) } ?: INACTIVE
    }
}
