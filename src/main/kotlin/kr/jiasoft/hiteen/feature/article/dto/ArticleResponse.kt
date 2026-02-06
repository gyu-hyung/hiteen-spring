package kr.jiasoft.hiteen.feature.article.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "공지사항/이벤트 응답 DTO")
data class ArticleResponse(
    @JsonIgnore
    @param:Schema(description = "게시글 PK (내부 관리용)", example = "1", hidden = true)
    val id: Long,

    @param:Schema(description = "카테고리 (NOTICE / EVENT)", example = "NOTICE")
    val category: String,

    @param:Schema(description = "제목", example = "긴급 공지사항")
    val subject: String? = null,

    @param:Schema(description = "내용", example = "서버 점검이 예정되어 있습니다.")
    val content: String? = null,

    @param:Schema(description = "외부 링크", example = "https://example.com")
    val link: String? = null,

    @param:Schema(description = "조회수", example = "125")
    val hits: Int = 0,

    @param:Schema(description = "첨부파일 UID 리스트 (NOTICE용)", example = "[\"550e8400-e29b-41d4-a716-446655441111\"]")
    val attachments: List<UUID>? = null,

    @param:Schema(description = "(EVENT) 큰 배너 UID 리스트", example = "[\"550e8400-e29b-41d4-a716-446655441111\"]")
    val largeBanners: List<UUID>? = null,

    @param:Schema(description = "(EVENT) 작은 배너 UID 리스트", example = "[\"550e8400-e29b-41d4-a716-446655441112\"]")
    val smallBanners: List<UUID>? = null,

    @param:Schema(description = "시작일", example = "2025-09-01")
    val startDate: LocalDate? = null,

    @param:Schema(description = "종료일", example = "2025-09-30")
    val endDate: LocalDate? = null,

    @param:Schema(description = "상태", example = "ACTIVE")
    val status: String,

    @param:Schema(description = "작성 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val createdAt: OffsetDateTime,

    @param:Schema(description = "수정 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val updatedAt: OffsetDateTime? = null,
)

