package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class AdminArticleListResponse(
    val id: Long,
    val category: String,
    val subject: String?,
    val content: String?,
    val link: String?,
    val ip: String?,
    val hits: Int,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val status: String,
    val createdId: Long,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime,
    val updatedId: Long?,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val updatedAt: OffsetDateTime?,
    val deletedId: Long?,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val deletedAt: OffsetDateTime?,
    val attachments: List<UUID>? = null,
    // 이벤트 배너 분리 응답
    val largeBanners: List<UUID>? = null,
    val smallBanners: List<UUID>? = null,
)
