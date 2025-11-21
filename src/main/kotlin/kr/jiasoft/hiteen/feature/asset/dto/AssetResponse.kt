package kr.jiasoft.hiteen.feature.asset.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.asset.domain.AssetEntity
import java.time.OffsetDateTime
import java.util.*

@Schema(description = "파일 응답 DTO")
data class AssetResponse(

    @JsonIgnore
    @param:Schema(description = "파일 고유 ID", example = "1")
    val id: Long,

    @param:Schema(description = "파일 고유 UID", example = "550e8400-e29b-41d4-a716-446655440000")
    val uid: UUID,

    @param:Schema(description = "업로드 시 원본 파일명", example = "profile.png")
    val originFileName: String,

    @param:Schema(description = "저장된 파일명 (내부 시스템용)", example = "a1b2c3d4_profile.png")
    val storedFileName: String,  // nameFileName

    @param:Schema(description = "서버 내 저장 경로", example = "/uploads/2025/09/18/a1b2c3d4_profile.png")
    val filePath: String,

    @param:Schema(description = "파일 MIME 타입", example = "image/png")
    val mimeType: String?,

    @param:Schema(description = "파일 크기 (bytes)", example = "204800")
    val size: Long,

    @param:Schema(description = "이미지 가로 길이(px)", example = "1080")
    val width: Int?,

    @param:Schema(description = "이미지 세로 길이(px)", example = "720")
    val height: Int?,

    @param:Schema(description = "파일 확장자", example = "png")
    val ext: String?,

    @param:Schema(description = "다운로드 횟수", example = "15")
    val downloadCount: Int,

    @param:Schema(description = "생성 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val createdAt: OffsetDateTime,
)

fun AssetEntity.toResponse() = AssetResponse(
    id = id,
    uid = requireNotNull(uid),
    originFileName = originFileName,
    storedFileName = storeFileName,
    filePath = filePath,
    mimeType = type,
    size = size,
    width = width,
    height = height,
    ext = ext,
    downloadCount = downloadCount,
    createdAt = createdAt
)
