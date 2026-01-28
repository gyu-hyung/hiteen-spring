package kr.jiasoft.hiteen.feature.asset.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import kr.jiasoft.hiteen.feature.asset.domain.AssetCategory
import kr.jiasoft.hiteen.feature.asset.domain.ThumbnailMode
import kr.jiasoft.hiteen.feature.asset.dto.StoredFile
import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.geometry.Positions
import org.springframework.http.codec.multipart.FilePart
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.util.*
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import org.slf4j.LoggerFactory

class AssetStorage(
    private val root: Path
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private fun normalizeExt(formatName: String?): String? = when (formatName?.lowercase()) {
        null -> null
        "jpeg", "jpg" -> "jpg"
        "png" -> "png"
        "gif" -> "gif"
        "bmp" -> "bmp"
        "webp" -> "webp"
        "wbmp" -> "wbmp"
        else -> formatName.lowercase()
    }

    private fun guessMimeByExt(ext: String?): String? = when (ext?.lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "bmp" -> "image/bmp"
        "svg" -> "image/svg+xml"
        else -> null
    }

    private fun detectImageFormat(path: Path): String? {
        return try {
            ImageIO.createImageInputStream(path.toFile()).use { iis ->
                val readers = ImageIO.getImageReaders(iis)
                if (readers.hasNext()) {
                    val reader: ImageReader = readers.next()
                    try {
                        normalizeExt(reader.formatName)
                    } finally {
                        try { reader.dispose() } catch (_: Throwable) {}
                    }
                } else null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun readImage(path: Path) =
        ImageIO.createImageInputStream(path.toFile()).use { iis ->
            val readers = ImageIO.getImageReaders(iis)
            if (!readers.hasNext()) return@use null
            val reader = readers.next()
            try {
                reader.input = iis
                reader.read(0)
            } finally {
                try { reader.dispose() } catch (_: Throwable) {}
            }
        }

    /**
     * 이미지 전체 디코드 없이(픽셀 로딩 없이) 메타데이터로만 width/height 추출
     * - 대용량 이미지 업로드 시 OOM 방지에 중요
     */
    private fun readImageDimensions(path: Path): Pair<Int?, Int?> {
        return try {
            ImageIO.createImageInputStream(path.toFile()).use { iis ->
                val readers = ImageIO.getImageReaders(iis)
                if (!readers.hasNext()) return (null to null)
                val reader = readers.next()
                try {
                    reader.input = iis
                    val w = reader.getWidth(0)
                    val h = reader.getHeight(0)
                    w to h
                } finally {
                    try { reader.dispose() } catch (_: Throwable) {}
                }
            }
        } catch (_: Throwable) {
            null to null
        }
    }

    /** 저장: 날짜 폴더/랜덤파일명. 확장자 유지 */
    suspend fun save(
        filePart: FilePart,
        allowedExts: List<String>,
        maxSizeBytes: Long,
        category: AssetCategory = AssetCategory.COMMON
    ): StoredFile {
        val t0 = System.nanoTime()

        val orig = filePart.filename()
        val extFromName = orig.substringAfterLast('.', "").lowercase().ifBlank { null }

        if (extFromName != null && allowedExts.isNotEmpty() && !allowedExts.contains(extFromName)) {
            throw IllegalArgumentException("허용되지 않은 확장자: .$extFromName")
        }

        // 파일을 임시로 받아 사이즈 검사
        val tmp = Files.createTempFile("upload-", ".${extFromName ?: "tmp"}")
        try {
            val tTransferStart = System.nanoTime()
            // transferTo는 non-blocking 처리가 되어 있음
            filePart.transferTo(tmp).awaitSingleOrNull()
            val tTransferMs = (System.nanoTime() - tTransferStart) / 1_000_000

            val size = Files.size(tmp)
            if (size > maxSizeBytes) throw IllegalArgumentException("파일 용량 초과: ${size}bytes")

            val tDetectStart = System.nanoTime()
            // ✅ 실제 이미지 포맷 기반으로 확장자 정정 (ex: webp인데 jpg로 들어오는 케이스)
            val detectedExt = detectImageFormat(tmp)
            val finalExt = detectedExt ?: extFromName
            val tDetectMs = (System.nanoTime() - tDetectStart) / 1_000_000

            if (finalExt != null && allowedExts.isNotEmpty() && !allowedExts.contains(finalExt)) {
                throw IllegalArgumentException("허용되지 않은 확장자(실제 포맷 기준): .$finalExt")
            }

            val today = LocalDate.now()
            val dir = root.resolve(
                "${category.fullPath()}/${today.year}/${"%02d".format(today.monthValue)}/${"%02d".format(today.dayOfMonth)}"
            )
            Files.createDirectories(dir)

            val randomName = UUID.randomUUID().toString().replace("-", "")
            val storedName = if (finalExt != null) "$randomName.$finalExt" else randomName
            val dest = dir.resolve(storedName)

            val tMoveStart = System.nanoTime()
            // 이동
            withContext(Dispatchers.IO) {
                Files.move(tmp, dest)
            }
            val tMoveMs = (System.nanoTime() - tMoveStart) / 1_000_000

            val tDimStart = System.nanoTime()
            // 이미지면 크기 추출 (✅ 전체 디코드 대신 메타데이터 기반)
            val (w, h) = readImageDimensions(dest)
            val tDimMs = (System.nanoTime() - tDimStart) / 1_000_000

            val tMimeStart = System.nanoTime()
            val mime = guessMimeByExt(finalExt)
                ?: try { Files.probeContentType(dest) } catch (_: Throwable) { null }
            val tMimeMs = (System.nanoTime() - tMimeStart) / 1_000_000

            val relDir = root.relativize(dir).toString().replace('\\', '/') + "/"

            val totalMs = (System.nanoTime() - t0) / 1_000_000
            log.debug(
                "[asset.save] file={} category={} size={} extName={} extDetected={} finalExt={} transferMs={} detectMs={} moveMs={} dimMs={} mimeMs={} totalMs={} dest={}",
                orig,
                category,
                size,
                extFromName,
                detectedExt,
                finalExt,
                tTransferMs,
                tDetectMs,
                tMoveMs,
                tDimMs,
                tMimeMs,
                totalMs,
                relDir + dest.fileName.toString(),
            )

            return StoredFile(
                relativePath = relDir,
                absolutePath = dest,
                originFileName = orig,
                ext = finalExt,
                size = size,
                width = w,
                height = h,
                mimeTypeGuess = mime
            )
        } catch (e: Throwable) {
            try { Files.deleteIfExists(tmp) } catch (_: Throwable) {}
            throw e
        }
    }

    private fun ensureWebpWriterAvailable() {
        val writers = ImageIO.getImageWritersByFormatName("webp")
        if (!writers.hasNext()) {
            throw IllegalStateException(
                "Specified format is not supported: webp (ImageIO writer not found). " +
                    "Add a WebP ImageIO writer (e.g. org.sejda.imageio:webp-imageio)."
            )
        }
    }

    private fun isMacArm64(): Boolean {
        val os = System.getProperty("os.name")?.lowercase() ?: ""
        val arch = System.getProperty("os.arch")?.lowercase() ?: ""
        return os.contains("mac") && (arch.contains("aarch64") || arch.contains("arm64"))
    }

    suspend fun createThumbnail(
        sourcePath: Path,
        ext: String,
        width: Int,
        height: Int,
        mode: ThumbnailMode = ThumbnailMode.COVER,
    ): StoredFile = withContext(Dispatchers.IO) {

        val srcExt = ext.lowercase()
        if (srcExt == "gif" || srcExt == "svg") {
            throw IllegalArgumentException("GIF, SVG는 리사이즈 불가")
        }

        // ✅ 기본은 webp로 생성하되, 로컬 mac arm64에서 네이티브 로딩 이슈가 있으면 jpg로 폴백
        var outExt = "webp"
        try {
            ensureWebpWriterAvailable()
        } catch (_: Throwable) {
            outExt = "jpg"
        }

        val today = LocalDate.now()
        val dir = root.resolve(
            "thumb/${mode.name.lowercase()}/${width}x${height}/${today.year}/${"%02d".format(today.monthValue)}/${"%02d".format(today.dayOfMonth)}"
        )
        Files.createDirectories(dir)

        val newName = "${UUID.randomUUID()}.$outExt"
        val dest = dir.resolve(newName)

        val srcImage = readImage(sourcePath)
            ?: throw IllegalArgumentException("지원되지 않는 이미지 포맷입니다.")

        val builder = Thumbnails.of(srcImage)
            .useExifOrientation(true)
            .outputFormat(outExt)
            .outputQuality(0.9)

        when (mode) {
            ThumbnailMode.COVER -> builder.crop(Positions.CENTER).size(width, height)
            ThumbnailMode.CONTAIN -> builder.size(width, height).keepAspectRatio(true)
        }

        try {
            builder.toFile(dest.toFile())
        } catch (e: UnsatisfiedLinkError) {
            // mac arm64에서 x86_64 dylib 로딩 실패 케이스
            if (outExt == "webp" && isMacArm64()) {
                outExt = "jpg"
                val dest2 = dest.parent.resolve(dest.fileName.toString().replaceAfterLast('.', outExt))
                Thumbnails.of(srcImage)
                    .useExifOrientation(true)
                    .outputFormat(outExt)
                    .outputQuality(0.9)
                    .apply {
                        when (mode) {
                            ThumbnailMode.COVER -> crop(Positions.CENTER).size(width, height)
                            ThumbnailMode.CONTAIN -> size(width, height).keepAspectRatio(true)
                        }
                    }
                    .toFile(dest2.toFile())
                return@withContext finalizeThumb(dest2, outExt, width, height)
            }
            throw e
        }

        finalizeThumb(dest, outExt, width, height)
    }

    private fun finalizeThumb(dest: Path, outExt: String, width: Int, height: Int): StoredFile {
        val mime = guessMimeByExt(outExt) ?: try { Files.probeContentType(dest) } catch (_: Throwable) { null }
        val size = Files.size(dest)
        val (outW, outH) = readImageDimensions(dest).let { (w, h) -> (w ?: width) to (h ?: height) }
        val relDir = root.relativize(dest.parent).toString().replace('\\', '/') + "/"
        return StoredFile(
            relativePath = relDir,
            absolutePath = dest,
            originFileName = dest.fileName.toString(),
            ext = outExt,
            size = size,
            width = outW,
            height = outH,
            mimeTypeGuess = mime
        )
    }


    suspend fun saveWordAsset(
        filePart: FilePart,
        word: String,
        allowedExts: List<String>,
        maxSizeBytes: Long,
        category: AssetCategory
    ): StoredFile {

        val orig = filePart.filename()
        val ext = orig.substringAfterLast('.', "").lowercase()
        if (ext.isBlank()) throw IllegalArgumentException("확장자가 없는 파일")

        if (allowedExts.isNotEmpty() && !allowedExts.contains(ext)) {
            throw IllegalArgumentException("허용되지 않은 확장자: .$ext")
        }

        // ✅ 파일명: 단어명 sanitize
        val safeWord = word
            .lowercase()
            .replace(Regex("[^a-z0-9_-]"), "")

        /** -------------------------
         * 물리 저장 경로
         * /srv/nfs/assets/word_img
         * ------------------------- */
        val physicalDir = root.resolve(category.fullPath())
        Files.createDirectories(physicalDir)

        val storedName = "$safeWord.$ext"
        val dest = physicalDir.resolve(storedName)

        val tmp = Files.createTempFile("upload-", ".$ext")

        try {
            filePart.transferTo(tmp).awaitSingleOrNull()

            val size = Files.size(tmp)
            if (size > maxSizeBytes) {
                throw IllegalArgumentException("파일 용량 초과: ${size}bytes")
            }

            withContext(Dispatchers.IO) {
                Files.move(
                    tmp,
                    dest,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                )
            }

            val (w, h) = readImageDimensions(dest)

            val mime = try {
                Files.probeContentType(dest)
            } catch (_: Throwable) {
                null
            }

            /** -------------------------
             * ⭐ 핵심: URL 기준 상대경로
             * /assets/word_img/honest.webp
             * ------------------------- */
            val relativePath = "/assets/${category.fullPath()}/"

            return StoredFile(
                relativePath = relativePath,
                absolutePath = dest,
                originFileName = orig,
                ext = ext,
                size = size,
                width = w,
                height = h,
                mimeTypeGuess = mime
            )
        } catch (e: Throwable) {
            try { Files.deleteIfExists(tmp) } catch (_: Throwable) {}
            throw e
        }
    }




    fun resolve(relativePath: String): Path = root.resolve(relativePath)
}
