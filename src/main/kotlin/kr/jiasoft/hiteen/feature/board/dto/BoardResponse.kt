package kr.jiasoft.hiteen.feature.board.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import kr.jiasoft.hiteen.common.dto.ApiPageCursor
import kr.jiasoft.hiteen.eloquent.annotation.BelongsTo
import kr.jiasoft.hiteen.eloquent.annotation.HasMany
import kr.jiasoft.hiteen.feature.user.dto.UserResponse
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID


@Table("boards")
data class BoardResponse (
    @Id
    @JsonIgnore
    val id : Long? = null,
    val uid: UUID? = null,
    val category: String? = null,
    val subject: String? = null,
    val content: String? = null,
    val link: String? = null,
    val hits: Int? = null,
    val assetUid: UUID? = null,
    val attachments: List<UUID>? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val status: String? = null,
    val address: String? = null,
    val detailAddress: String? = null,
    val createdAt: OffsetDateTime? = null,
    val createdId: Long? = null,
    val updatedAt: OffsetDateTime? = null,
    val likeCount: Long? = null,
    val commentCount: Long? = null,
    val likedByMe: Boolean? = null,

    @BelongsTo(target = UserResponse::class, foreignKey = "createdId")
    val user: UserResponse? = null,

    @HasMany(target = BoardCommentResponse::class, foreignKey = "boardId")
    val comments: ApiPageCursor<BoardCommentResponse>? = null,
)