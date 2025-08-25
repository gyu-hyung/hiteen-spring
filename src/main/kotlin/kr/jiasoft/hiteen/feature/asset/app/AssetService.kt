package kr.jiasoft.hiteen.feature.asset.app

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.asset.domain.AssetEntity
import kr.jiasoft.hiteen.feature.asset.dto.AssetResponse
import kr.jiasoft.hiteen.feature.asset.dto.toResponse
import kr.jiasoft.hiteen.feature.asset.infra.AssetRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

@Service
class AssetService(
    private val assetRepository: AssetRepository,

    @Value("\${app.asset.storage-root}") private val storageRoot: String,
    @Value("\${app.asset.allowed-exts}") private val allowedExtsCsv: String,
    @Value("\${app.asset.max-size-bytes}") private val maxSizeBytes: Long,
) {

    // CSV → List<String>, 공백 제거 + 소문자 통일 + 빈 항목 제거
    private val allowedExts: List<String> =
        allowedExtsCsv.split(',').mapNotNull { it.trim().lowercase().ifBlank { null } }

    private val root: Path = Path.of(storageRoot).also { Files.createDirectories(it) }
    private val storage = AssetStorage(root)

    suspend fun upload(file: FilePart, originFileName: String?, currentUserId: Long): AssetResponse {
        val stored = storage.save(file, allowedExts, maxSizeBytes)
        val entity = AssetEntity(
            originFileName = originFileName ?: file.filename(),
            storeFileName = stored.absolutePath.fileName.toString(),
            filePath = stored.relativePath,
            type = stored.mimeTypeGuess,
            size = stored.size,
            width = stored.width,
            height = stored.height,
            ext = stored.ext,
            createdId = currentUserId
        )
        val saved = assetRepository.save(entity)
        return saved.toResponse()
    }

    suspend fun get(uid: UUID): AssetEntity? =
        assetRepository.findByUid(uid)?.takeIf { it.deletedAt == null }

    fun list(limit: Int, offset: Int): Flow<AssetEntity> =
        assetRepository.listAlive(limit.coerceIn(1, 100), offset.coerceAtLeast(0))

    suspend fun increaseDownload(uid: UUID, userId: Long): AssetEntity? =
        assetRepository.increaseDownloadAndReturn(uid, userId)

    suspend fun softDelete(uid: UUID, userId: Long): AssetEntity? =
        assetRepository.softDelete(uid, userId)

    fun resolveFilePath(relative: String): Path = storage.resolve(relative)

    fun existsFile(path: Path): Boolean = Files.exists(path)
}
