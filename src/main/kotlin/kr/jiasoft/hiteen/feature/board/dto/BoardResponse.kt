package kr.jiasoft.hiteen.feature.board.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.common.dto.ApiPageCursor
import kr.jiasoft.hiteen.eloquent.annotation.BelongsTo
import kr.jiasoft.hiteen.eloquent.annotation.HasMany
import kr.jiasoft.hiteen.feature.user.dto.UserSummary
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "게시글 응답 DTO")
@Table("boards")
data class BoardResponse(

    @Id
    @JsonIgnore
    @param:Schema(description = "게시글 PK (내부 관리용)", example = "1", hidden = true)
    val id: Long,

    @param:Schema(description = "게시글 UID", example = "550e8400-e29b-41d4-a716-446655440000")
    val uid: UUID,

    @param:Schema(description = "카테고리", example = "공지사항")
    val category: String,

    @param:Schema(description = "제목", example = "학교 행사 안내")
    val subject: String,

    @param:Schema(description = "내용", example = "이번 주 금요일에 체육대회가 열립니다.")
    val content: String,

    @param:Schema(description = "외부 링크", example = "https://example.com")
    val link: String? = null,

    @param:Schema(description = "조회수", example = "125")
    val hits: Int = 0,

    @param:Schema(description = "대표 첨부파일 UID", example = "550e8400-e29b-41d4-a716-446655441111")
    val assetUid: UUID? = null,

    @param:Schema(description = "첨부파일 UID 리스트", example = "[\"550e8400-e29b-41d4-a716-446655441111\"]")
    val attachments: List<UUID>? = null,

    @param:Schema(description = "시작일", example = "2025-09-01")
    val startDate: LocalDate? = null,

    @param:Schema(description = "종료일", example = "2025-09-30")
    val endDate: LocalDate? = null,

    @param:Schema(description = "상태", example = "ACTIVE")
    val status: String,

    @param:Schema(description = "주소", example = "서울특별시 강남구 테헤란로 123")
    val address: String?,

    @param:Schema(description = "상세 주소", example = "빌딩 5층")
    val detailAddress: String?,

    @param:Schema(description = "작성 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val createdAt: OffsetDateTime,

    @param:Schema(description = "작성자 ID (내부용)", example = "1001", hidden = true)
    val createdId: Long,

    @param:Schema(description = "수정 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val updatedAt: OffsetDateTime? = null,

    @param:Schema(description = "좋아요 수", example = "42")
    val likeCount: Long = 0,

    @param:Schema(description = "댓글 수", example = "12")
    val commentCount: Long = 0,

    @param:Schema(description = "내가 좋아요 눌렀는지 여부", example = "true")
    val likedByMe: Boolean? = false,

    @BelongsTo(target = UserSummary::class, foreignKey = "createdId")
    @param:Schema(description = "작성자 요약 정보")
    val user: UserSummary? = null,

    @HasMany(target = BoardCommentResponse::class, foreignKey = "boardId")
    @param:Schema(description = "댓글 목록 (커서 기반 페이지네이션)")
    val comments: ApiPageCursor<BoardCommentResponse>? = null,
)
