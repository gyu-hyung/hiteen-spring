package kr.jiasoft.hiteen.feature.asset.app

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.jiasoft.hiteen.feature.asset.domain.AssetCategory
import kr.jiasoft.hiteen.feature.asset.domain.AssetEntity
import kr.jiasoft.hiteen.feature.asset.domain.ThumbnailMode
import kr.jiasoft.hiteen.feature.asset.dto.AssetResponse
import kr.jiasoft.hiteen.feature.asset.dto.StoredFile
import kr.jiasoft.hiteen.feature.asset.dto.toResponse
import kr.jiasoft.hiteen.feature.asset.infra.AssetRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import org.slf4j.LoggerFactory
import kotlinx.coroutines.withTimeout
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.oned.Code128Writer
import javax.imageio.ImageIO
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage

@Service
class AssetService(
    private val assetRepository: AssetRepository,

    @Value("\${app.asset.storage-root}") private val storageRoot: String,
    @Value("\${app.asset.allowed-exts}") private val allowedExtsCsv: String,
    @Value("\${app.asset.max-size-bytes}") private val maxSizeBytes: Long,
) {

    private val log = LoggerFactory.getLogger(javaClass)

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
        currentUserId: Long,
        category: AssetCategory = AssetCategory.COMMON,
        originFileName: String? = null,
    ): AssetResponse {
        val t0 = System.nanoTime()

        val tSaveStart = System.nanoTime()
        val stored = storage.save(file, allowedImageExts, maxSizeBytes, category)
        val tSaveMs = (System.nanoTime() - tSaveStart) / 1_000_000

        val tEnsureStart = System.nanoTime()
        ensureImageOrDelete(stored)
        val tEnsureMs = (System.nanoTime() - tEnsureStart) / 1_000_000

        val tDbStart = System.nanoTime()
        val res = uploadStored(stored, originFileName, currentUserId)
        val tDbMs = (System.nanoTime() - tDbStart) / 1_000_000

        val totalMs = (System.nanoTime() - t0) / 1_000_000
        log.debug(
            "[uploadImage] userId={} category={} file={} size={} saveMs={} ensureMs={} dbMs={} totalMs={} storedPath={}",
            currentUserId,
            category,
            file.filename(),
            stored.size,
            tSaveMs,
            tEnsureMs,
            tDbMs,
            totalMs,
            stored.relativePath + stored.absolutePath.fileName.toString(),
        )

        return res
    }

    suspend fun uploadWordAsset(
        file: FilePart,
        word: String,
        currentUserId: Long,
        category: AssetCategory, // WORD_IMG or SOUND
        isImage: Boolean,
    ): AssetResponse {

        val stored = storage.saveWordAsset(
            filePart = file,
            word = word,
            allowedExts = if (isImage) allowedImageExts else allowedExts,
            maxSizeBytes = maxSizeBytes,
            category = category
        )

        if (isImage) {
            ensureImageOrDelete(stored)
        }

        return uploadStored(
            stored = stored,
            originFileName = file.filename(),
            currentUserId = currentUserId
        )
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

    /** 여러 이미지 업로드 (이미지 유효성 검사 포함)
     *
     * 주의: uploadImage() 내부에서 imageWorkSemaphore.withPermit 을 이미 사용하므로,
     * 여기서 다시 withPermit 을 걸면 (permits=2 기준) 중첩 획득으로 인해 데드락/무한대기가 발생할 수 있습니다.
     */
    suspend fun uploadImages(
        files: List<FilePart>,
        currentUserId: Long,
        category: AssetCategory = AssetCategory.COMMON,
        originFileNames: List<String>? = null
    ): List<AssetResponse> {
        val t0 = System.nanoTime()

        // 안전장치: 업로드가 무한정 대기하지 않도록 전체에 타임아웃
        val res = withTimeout(5 * 60 * 1000L) {
            coroutineScope {
                files.mapIndexed { idx, f ->
                    async {
                        val origin = originFileNames?.getOrNull(idx)
                        // ✅ uploadImage가 내부에서 semaphore/검증/저장을 처리
                        uploadImage(f, currentUserId, category, origin)
                    }
                }.awaitAll()
            }
        }

        val totalMs = (System.nanoTime() - t0) / 1_000_000
        log.debug(
            "[uploadImages] userId={} category={} count={} totalMs={} filenames={}",
            currentUserId,
            category,
            files.size,
            totalMs,
            files.map { it.filename() }
        )
        return res
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

    suspend fun getOrCreateThumbnail(
        uid: UUID,
        width: Int,
        height: Int,
        currentUserId: Long? = null,
        mode: ThumbnailMode = ThumbnailMode.COVER,
    ): AssetEntity {
        // 1️⃣ 원본 조회 (DB → non-blocking OK)
        val original = findByUid(uid)
            ?: throw IllegalArgumentException("존재하지 않는 파일 (uid=$uid)")

        // 2️⃣ 기존 썸네일 재사용
        val existing = assetRepository.findByOriginAndSize(original.id, width, height)
        if (existing != null) return existing

        // 3️⃣ 원본 파일 검증
        val originalPath = resolveFilePath(original.filePath + original.storeFileName)
        if (!existsFile(originalPath)) {
            throw IllegalArgumentException("원본 파일 없음 (assetId=${original.id})")
        }

        // 4️⃣ 확장자 검증 (NPE 방지)
        val ext = original.ext?.lowercase()
            ?: throw IllegalArgumentException(
                "확장자가 없는 파일은 썸네일 생성 불가 (assetId=${original.id})"
            )

        if (ext in listOf("gif", "svg")) {
            throw IllegalArgumentException("GIF, SVG는 리사이즈 불가 (ext=$ext)")
        }

        val resizedStored = storage.createThumbnail(
            sourcePath = originalPath,
            ext = ext,
            width = width,
            height = height,
            mode = mode,
        )

        // 6️⃣ 파일명 안전 생성
        val baseName = original.originFileName.substringBeforeLast('.')

        // 7️⃣ 엔티티 생성
        val entity = AssetEntity(
            originFileName = "${baseName}_${width}x${height}.${resizedStored.ext}",
            storeFileName = resizedStored.absolutePath.fileName.toString(),
            filePath = resizedStored.relativePath,
            type = resizedStored.mimeTypeGuess,
            size = resizedStored.size,
            width = resizedStored.width,
            height = resizedStored.height,
            originId = original.id,
            ext = resizedStored.ext,
            createdId = currentUserId,
            createdAt = OffsetDateTime.now(),
        )

        // 8️⃣ 저장 (동시 생성 대비)
        return try {
            assetRepository.save(entity)
        } catch (e: Exception) {
            assetRepository.findByOriginAndSize(original.id, width, height)
                ?: throw e
        }
    }


    suspend fun findByUid(uid: UUID): AssetEntity? = assetRepository.findByUid(uid)

    suspend fun get(uid: UUID): AssetEntity? =
        assetRepository.findByUid(uid)?.takeIf { it.deletedAt == null }

    fun list(limit: Int, offset: Int): Flow<AssetEntity> =
        assetRepository.listAlive(limit.coerceIn(1, 100), offset.coerceAtLeast(0))

    suspend fun increaseDownload(uid: UUID, userId: Long): AssetEntity? =
        assetRepository.increaseDownloadAndReturn(uid, userId)

    suspend fun increase(uid: UUID): AssetEntity? =
        assetRepository.increaseDownloadCount(uid)

    suspend fun softDelete(uid: UUID, userId: Long): AssetEntity? =
        assetRepository.softDelete(uid, userId)

    fun resolveFilePath(relative: String): Path = storage.resolve(relative)

    fun existsFile(path: Path): Boolean = Files.exists(path)

    /**
     * PIN 번호로 바코드 이미지(CODE_128)를 생성하여 저장하고 AssetResponse 반환
     * - 바코드 상하단에 여백, 하단에 PIN 번호 텍스트 표시
     * @param pinNo 바코드로 인코딩할 PIN 번호
     * @param currentUserId 생성자 사용자 ID
     * @param width 바코드 이미지 너비 (기본값: 600)
     * @param barcodeHeight 바코드 영역 높이 (기본값: 150)
     * @param topPadding 상단 여백 (기본값: 30)
     * @param textHeight 텍스트 영역 높이 (기본값: 50)
     * @return 저장된 바코드 이미지의 AssetResponse
     */
    suspend fun createBarcodeImage(
        pinNo: String,
        currentUserId: Long,
        width: Int = 600,
        barcodeHeight: Int = 150,
        topPadding: Int = 30,
        textHeight: Int = 50,
    ): AssetResponse = withContext(Dispatchers.IO) {
        val t0 = System.nanoTime()

        val totalHeight = topPadding + barcodeHeight + textHeight

        // 1 바코드 생성 (CODE_128 포맷)
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 10
        )
        val writer = Code128Writer()
        val bitMatrix = writer.encode(pinNo, BarcodeFormat.CODE_128, width, barcodeHeight, hints)
        val barcodeImage = MatrixToImageWriter.toBufferedImage(bitMatrix)

        // 2 상단 패딩 + 바코드 + 텍스트 영역을 포함한 최종 이미지 생성
        val finalImage = BufferedImage(width, totalHeight, BufferedImage.TYPE_INT_RGB)
        val g2d = finalImage.createGraphics()

        // 안티앨리어싱 설정
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        // 배경을 흰색으로
        g2d.color = Color.WHITE
        g2d.fillRect(0, 0, width, totalHeight)

        // 바코드 이미지 그리기 (상단 패딩만큼 아래로)
        g2d.drawImage(barcodeImage, 0, topPadding, null)

        // 3 PIN 번호 텍스트 그리기
        g2d.color = Color.BLACK
        val fontSize = (textHeight * 0.5).toInt().coerceIn(16, 32)
        g2d.font = Font("SansSerif", Font.BOLD, fontSize)

        val fm = g2d.fontMetrics
        val textWidth = fm.stringWidth(pinNo)
        val textX = (width - textWidth) / 2
        val textY = topPadding + barcodeHeight + ((textHeight + fm.ascent - fm.descent) / 2)

        g2d.drawString(pinNo, textX, textY)
        g2d.dispose()

        // 4 저장 경로 생성
        val today = LocalDate.now()
        val category = AssetCategory.BARCODE
        val dir = resolveFilePath(
            "${category.fullPath()}/${today.year}/${"%02d".format(today.monthValue)}/${"%02d".format(today.dayOfMonth)}"
        )
        Files.createDirectories(dir)

        val randomName = UUID.randomUUID().toString().replace("-", "")
        val storedName = "$randomName.png"
        val dest = dir.resolve(storedName)

        // 5️⃣ 이미지 파일로 저장
        ImageIO.write(finalImage, "png", dest.toFile())
        val size = Files.size(dest)

        // 6️⃣ 상대 경로 계산
        val relDir = resolveFilePath("").relativize(dir).toString().replace('\\', '/') + "/"

        // 7️⃣ AssetEntity 생성 및 저장
        val entity = AssetEntity(
            originFileName = "barcode_$pinNo.png",
            storeFileName = storedName,
            filePath = relDir,
            type = "image/png",
            size = size,
            width = width,
            height = totalHeight,
            ext = "png",
            createdId = currentUserId,
            createdAt = OffsetDateTime.now(),
        )
        val saved = assetRepository.save(entity)

        val totalMs = (System.nanoTime() - t0) / 1_000_000
        log.debug(
            "[createBarcodeImage] pinNo={} userId={} size={} totalMs={} storedPath={}",
            pinNo,
            currentUserId,
            size,
            totalMs,
            relDir + storedName,
        )

        saved.toResponse()
    }
}