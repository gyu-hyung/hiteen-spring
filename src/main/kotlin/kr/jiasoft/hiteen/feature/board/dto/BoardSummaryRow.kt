package kr.jiasoft.hiteen.feature.board.dto

import org.springframework.data.relational.core.mapping.Column
import java.time.OffsetDateTime
import java.util.UUID

data class BoardSummaryRow(
    val uid: UUID,
    val category: String?,
    val subject: String?,
    val content: String?,
    val link: String?,
    val hits: Int?,
    @Column("asset_uid")
    val assetUid: UUID?,
    val createdAt: OffsetDateTime?,
    val createdId: Long,
    val likeCount: Long,
    val commentCount: Long,
    val likedByMe: Boolean
)