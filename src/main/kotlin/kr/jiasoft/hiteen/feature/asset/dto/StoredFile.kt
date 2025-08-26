package kr.jiasoft.hiteen.feature.asset.dto

import java.nio.file.Path

data class StoredFile(
    val relativePath: String,   // 예: 2025/08/25/xxxxxx.jpg
    val absolutePath: Path,
    val ext: String?,
    val size: Long,
    val width: Int?,
    val height: Int?,
    val mimeTypeGuess: String?
)