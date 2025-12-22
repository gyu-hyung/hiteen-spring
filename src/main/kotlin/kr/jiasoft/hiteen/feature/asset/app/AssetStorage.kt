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
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.util.*
import javax.imageio.ImageIO

class AssetStorage(
    private val root: Path
) {
    /** 저장: 날짜 폴더/랜덤파일명. 확장자 유지 */
    suspend fun save(
        filePart: FilePart,
        allowedExts: List<String>,
        maxSizeBytes: Long,
        category: AssetCategory = AssetCategory.COMMON
    ): StoredFile {
        val orig = filePart.filename()
        val ext = orig.substringAfterLast('.', "").lowercase().ifBlank { null }

        if (ext != null && allowedExts.isNotEmpty() && !allowedExts.contains(ext)) {
            throw IllegalArgumentException("허용되지 않은 확장자: .$ext")
        }

        // 파일을 임시로 받아 사이즈 검사
        val tmp = Files.createTempFile("upload-", ".$ext")
        try {
            // transferTo는 non-blocking 처리가 되어 있음
            filePart.transferTo(tmp).awaitSingleOrNull()
            val size = Files.size(tmp)
            if (size > maxSizeBytes) throw IllegalArgumentException("파일 용량 초과: ${size}bytes")

            val today = LocalDate.now()
            val dir = root.resolve(
                "${category.fullPath()}/${today.year}/${"%02d".format(today.monthValue)}/${"%02d".format(today.dayOfMonth)}"
            )
            Files.createDirectories(dir)

            val randomName = UUID.randomUUID().toString().replace("-", "")
            val storedName = if (ext != null) "$randomName.$ext" else randomName
            val dest = dir.resolve(storedName)

            // 이동
            withContext(Dispatchers.IO) {
                Files.move(tmp, dest)
            }

            // 이미지면 크기 추출
            val (w, h) = try {
                ImageIO.read(dest.toFile())?.let { it.width to it.height } ?: (null to null)
            } catch (_: Throwable) { null to null }

            val mime = try { Files.probeContentType(dest) } catch (_: Throwable) { null }

            val relDir = root.relativize(dir).toString().replace('\\', '/') + "/"

            return StoredFile(
                relativePath = relDir,
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

    suspend fun createThumbnail(
        sourcePath: Path,
        ext: String,
        width: Int,
        height: Int,
        mode: ThumbnailMode = ThumbnailMode.COVER,
    ): StoredFile = withContext(Dispatchers.IO) {

        val safeExt = ext.lowercase()
        if (safeExt == "gif" || safeExt == "svg") {
            throw IllegalArgumentException("GIF, SVG는 리사이즈 불가")
        }

        val today = LocalDate.now()
        val dir = root.resolve(
            "thumb/${mode.name.lowercase()}/${width}x${height}/${today.year}/${"%02d".format(today.monthValue)}/${"%02d".format(today.dayOfMonth)}"
        )
        Files.createDirectories(dir)

        val newName = "${UUID.randomUUID()}.$safeExt"
        val dest = dir.resolve(newName)

        val builder = Thumbnails.of(sourcePath.toFile())
            .useExifOrientation(true)    // ✅ 회전 문제 해결(핵심)
            .outputFormat(safeExt)
            .outputQuality(0.9)          // 필요하면 조절

        when (mode) {
            ThumbnailMode.COVER -> {
                // ✅ 비율 유지 + 중앙 크롭 → 결과는 정확히 width x height
                builder
                    .crop(Positions.CENTER)
                    .size(width, height)
            }
            ThumbnailMode.CONTAIN -> {
                // ✅ 비율 유지 + 전체가 보이게 → 한 변이 덜 찰 수 있음(여백은 프론트에서 object-fit: contain)
                builder
                    .size(width, height)
                    .keepAspectRatio(true)
            }
        }

        builder.toFile(dest.toFile())

        val mime = try { Files.probeContentType(dest) } catch (_: Throwable) { null }
        val size = Files.size(dest)

        // 실제 생성된 크기 다시 읽기(정확도)
        val (outW, outH) = try {
            javax.imageio.ImageIO.read(dest.toFile())?.let { it.width to it.height } ?: (width to height)
        } catch (_: Throwable) { width to height }

        val relDir = root.relativize(dir).toString().replace('\\', '/') + "/"

        StoredFile(
            relativePath = relDir,
            absolutePath = dest,
            originFileName = newName,
            ext = safeExt,
            size = size,
            width = outW,
            height = outH,
            mimeTypeGuess = mime
        )
    }




    fun resolve(relativePath: String): Path = root.resolve(relativePath)
}
