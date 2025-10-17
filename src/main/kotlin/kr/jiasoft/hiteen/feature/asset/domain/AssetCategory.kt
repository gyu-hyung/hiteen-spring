package kr.jiasoft.hiteen.feature.asset.domain

enum class AssetCategory(val folderName: String) {
    PROFILE("PROFILE"),
    POST("POST"),
    COMMON("COMMON"),
    CODE("CODE"),
    POLL("POLL");

    companion object {
        fun fromName(name: String?): AssetCategory =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: COMMON
    }
}
