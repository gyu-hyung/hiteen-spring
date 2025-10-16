package kr.jiasoft.hiteen.feature.asset.app

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.asset.domain.AssetCategory
import kr.jiasoft.hiteen.feature.asset.domain.AssetEntity
import kr.jiasoft.hiteen.feature.asset.dto.AssetResponse
import kr.jiasoft.hiteen.feature.asset.dto.StoredFile
import kr.jiasoft.hiteen.feature.asset.dto.toResponse
import kr.jiasoft.hiteen.feature.asset.infra.AssetRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.time.OffsetDateTime
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

    // ✅ 이미지 전용 확장자
    private val imageExts = setOf("jpg","jpeg","png","gif","webp","bmp","svg")

    // allowedExts 중 이미지에 해당하는 것만 허용
    private val allowedImageExts: List<String> =
        allowedExts.filter { it in imageExts }.ifEmpty { imageExts.toList() } // 설정이 비어있으면 기본 이미지 확장자 사용

    private val root: Path = Path.of(storageRoot).also { Files.createDirectories(it) }
    private val storage = AssetStorage(root)





    suspend fun upload(
        file: FilePart,
        originFileName: String?,
        currentUserId: Long,
        category: AssetCategory = AssetCategory.COMMON
    ): AssetResponse {
        val stored = storage.save(file, allowedExts, maxSizeBytes, category)
        return uploadStored(stored, originFileName ?: file.filename(), currentUserId) // 저장/DB 로직 단일화
    }

    suspend fun uploadImage(
        file: FilePart,
        originFileName: String?,
        currentUserId: Long,
        category: AssetCategory = AssetCategory.COMMON
    ): AssetResponse {
        val stored = storage.save(file, allowedImageExts, maxSizeBytes, category)
        ensureImageOrDelete(stored)
        return uploadStored(stored, originFileName, currentUserId)
    }

    /** 여러 파일 업로드 (일반 파일) */
    suspend fun uploadAll(
        files: List<FilePart>,
        currentUserId: Long,
        originFileNames: List<String>? = null
    ): List<AssetResponse> = coroutineScope {
        files.mapIndexed { idx, f ->
            async {
                val origin = originFileNames?.getOrNull(idx)
                upload(f, origin, currentUserId)
            }
        }.awaitAll()
    }

    /** 여러 이미지 업로드 (이미지 유효성 검사 포함) */
    suspend fun uploadImages(
        files: List<FilePart>,
        currentUserId: Long,
        originFileNames: List<String>? = null
    ): List<AssetResponse> = coroutineScope {
        files.mapIndexed { idx, f ->
            async {
                val origin = originFileNames?.getOrNull(idx)
                uploadImage(f, origin, currentUserId)  // 이미지 전용 wrapper 사용
            }
        }.awaitAll()
    }

    /** ⬇️ 저장/DB 로직을 한 곳에 모음: upload / uploadImage 둘 다 여기로 온다 */
    private suspend fun uploadStored(stored: StoredFile, originFileName: String?, currentUserId: Long): AssetResponse {
        val entity = AssetEntity(
            originFileName = originFileName ?: stored.originFileName,
            storeFileName = stored.absolutePath.fileName.toString(), // @Column("name_file_name") 매핑 확인
            filePath = stored.relativePath,
            type = stored.mimeTypeGuess,
            size = stored.size,
            width = stored.width,
            height = stored.height,
            ext = stored.ext,
            createdId = currentUserId,
            createdAt = OffsetDateTime.now(),
        )
        val saved = assetRepository.save(entity)
        return saved.toResponse()
    }

    /** 이미지 판별: 실패 시 물리 파일 삭제 후 예외 */
    private fun ensureImageOrDelete(stored: StoredFile) {
        val isImageMime = stored.mimeTypeGuess?.lowercase()?.startsWith("image/") == true
        val hasDims = stored.width != null && stored.height != null
        val isSvg = stored.ext?.lowercase() == "svg"

        if (!isImageMime && !hasDims && !isSvg) {
            Files.deleteIfExists(stored.absolutePath)
            throw IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.")
        }
    }

    suspend fun get(uid: UUID): AssetEntity? =
        assetRepository.findByUid(uid)?.takeIf { it.deletedAt == null }

    fun list(limit: Int, offset: Int): Flow<AssetEntity> =
        assetRepository.listAlive(limit.coerceIn(1, 100), offset.coerceAtLeast(0))

    suspend fun increaseDownload(uid: UUID, userId: Long): AssetEntity? =
        assetRepository.increaseDownloadAndReturn(uid, userId)

    suspend fun increase(uid: UUID): AssetEntity? =
        assetRepository.increaseDownload(uid)

    suspend fun softDelete(uid: UUID, userId: Long): AssetEntity? =
        assetRepository.softDelete(uid, userId)

    fun resolveFilePath(relative: String): Path = storage.resolve(relative)

    fun existsFile(path: Path): Boolean = Files.exists(path)
}
