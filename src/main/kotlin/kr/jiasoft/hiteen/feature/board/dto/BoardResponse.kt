package kr.jiasoft.hiteen.feature.board.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import kr.jiasoft.hiteen.common.dto.ApiPageCursor
import kr.jiasoft.hiteen.eloquent.annotation.BelongsTo
import kr.jiasoft.hiteen.eloquent.annotation.HasMany
import kr.jiasoft.hiteen.feature.user.dto.UserSummary
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID


@Table("boards")
data class BoardResponse (
    @Id
    @JsonIgnore
    val id : Long,
    val uid: UUID,
    val category: String,
    val subject: String,
    val content: String,
    val link: String? = null,
    val hits: Int = 0,
    val assetUid: UUID? = null,
    val attachments: List<UUID>? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val status: String,
    val address: String?,
    val detailAddress: String?,
    val createdAt: OffsetDateTime,
    val createdId: Long,
    val updatedAt: OffsetDateTime? = null,
    val likeCount: Long = 0,
    val commentCount: Long = 0,
    val likedByMe: Boolean? = false,

    @BelongsTo(target = UserSummary::class, foreignKey = "createdId")
    val user: UserSummary? = null,

    @HasMany(target = BoardCommentResponse::class, foreignKey = "boardId")
    val comments: ApiPageCursor<BoardCommentResponse>? = null,
)