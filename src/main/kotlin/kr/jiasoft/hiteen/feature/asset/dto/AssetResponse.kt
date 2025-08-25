package kr.jiasoft.hiteen.feature.asset.dto

import kr.jiasoft.hiteen.feature.asset.domain.AssetEntity
import java.time.OffsetDateTime
import java.util.*

data class AssetResponse(
    val uid: UUID,
    val originFileName: String?,
    val storedFileName: String?,  // nameFileName
    val filePath: String?,
    val mimeType: String?,
    val size: Long?,
    val width: Int?,
    val height: Int?,
    val ext: String?,
    val downloadCount: Int,
    val createdAt: OffsetDateTime?,
)

fun AssetEntity.toResponse() = AssetResponse(
    uid = requireNotNull(uid),
    originFileName = originFileName,
    storedFileName = storeFileName,
    filePath = filePath,
    mimeType = type,
    size = size,
    width = width,
    height = height,
    ext = ext,
    downloadCount = downloadCount ?: 0,
    createdAt = createdAt
)
