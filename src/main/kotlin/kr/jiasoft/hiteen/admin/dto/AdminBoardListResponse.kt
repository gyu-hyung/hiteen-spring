package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.OffsetDateTime
import java.time.LocalDate
import java.util.UUID

data class AdminBoardListResponse (
    val id: Long,
    val uid: String,
    val category: String,
    val subject: String?,
    val content: String?,
    val link: String?,
    val ip: String?,
    val hits: Int,
    val assetUid: String?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val reportCount: Int,
    val commentCount: Int,
    val likeCount: Int,
    val status: String,
    val address: String?,
    val detailAddress: String?,
    val lat: Double?,
    val lng: Double?,
    val createdId: Long,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime,
    val updatedId: Long?,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val updatedAt: OffsetDateTime?,
    val deletedId: Long?,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val deletedAt: OffsetDateTime?,
    val createdUid: UUID?,
    val nickname: String?,
    val attachments: List<UUID>? = null,

    // 이벤트 배너 분리 응답(호환을 위해 attachments는 유지)
    val largeBanners: List<UUID>? = null,
    val smallBanners: List<UUID>? = null,
)