package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Schema(description = "배너 DTO")
data class AdminBannerResponse(
    val id: Long,
    val uid: UUID?,
    val category: String,
    val title: String? = null,
    val linkType: String? = null,
    val linkUrl: String? = null,
    val bbsCode: String? = null,
    val bbsId: Int? = null,
    val assetUid: UUID? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val status: String,

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val updatedAt: OffsetDateTime?,

    @get:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yy.MM.dd HH:mm")
    val createdDate: String? = createdAt.format(DateTimeFormatter.ofPattern("yy.MM.dd HH:mm")),
    @get:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yy.MM.dd HH:mm")
    val updatedDate: String? = updatedAt?.format(DateTimeFormatter.ofPattern("yy.MM.dd HH:mm")),
)

