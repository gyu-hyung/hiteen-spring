package kr.jiasoft.hiteen.feature.asset.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import kr.jiasoft.hiteen.feature.asset.domain.AssetCategory
import kr.jiasoft.hiteen.feature.asset.dto.StoredFile
import org.springframework.http.codec.multipart.FilePart
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

    fun resolve(relativePath: String): Path = root.resolve(relativePath)
}
