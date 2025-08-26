package kr.jiasoft.hiteen.feature.board.dto

import java.time.OffsetDateTime
import java.util.UUID

data class BoardDetailResponse(
    val uid: UUID,
    val category: String?,
    val subject: String,
    val content: String,
    val link: String?,
    val hits: Int,
    val assetUid: UUID?,
    val attachments: List<UUID>,
    val startDate: OffsetDateTime?,
    val endDate: OffsetDateTime?,
    val status: String?,
    val address: String?,
    val detailAddress: String?,
    val createdAt: OffsetDateTime?,
    val createdId: Long,
    val updatedAt: OffsetDateTime?,
    val likeCount: Long,
    val commentCount: Long,
    val likedByMe: Boolean,
)