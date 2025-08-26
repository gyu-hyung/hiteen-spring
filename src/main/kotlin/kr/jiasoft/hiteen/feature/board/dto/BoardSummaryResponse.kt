package kr.jiasoft.hiteen.feature.board.dto

import java.time.OffsetDateTime
import java.util.UUID

data class BoardSummaryResponse(
    val uid: UUID,
    val category: String?,
    val subject: String,
    val contentPreview: String,
    val link: String?,
    val hits: Int,
    val assetUid: UUID?,
    val createdAt: OffsetDateTime?,
    val createdId: Long,
    val likeCount: Long,
    val commentCount: Long,
    val likedByMe: Boolean,
)