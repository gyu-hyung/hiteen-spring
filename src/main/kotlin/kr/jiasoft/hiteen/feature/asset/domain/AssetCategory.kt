package kr.jiasoft.hiteen.feature.asset.domain

enum class AssetCategory(
    val basePath: String,  // 1차 도메인
    val subPath: String? = null // 2차 세부 구분
) {
    PROFILE("profile"),
    USER_PHOTO("user-photo"),
    POST("post"),
    POST_THUMBNAIL("post", "thumbnail"),
    POST_ATTACHMENT("post", "attachment"),
    COMMON("common"),
    CODE("code"),
    POLL_MAIN("poll", "main"),
    POLL_SELECT("poll", "select"),
    POLL_COMMENT("poll", "comment");

    fun fullPath(): String =
        if (subPath != null) "$basePath/$subPath" else basePath

    companion object {
        fun fromName(name: String?): AssetCategory =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: COMMON
    }
}
