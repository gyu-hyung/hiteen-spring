package kr.jiasoft.hiteen.feature.article.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "이전글/다음글 네비게이션 정보")
data class ArticleNavigation(
    @field:Schema(description = "게시글 ID", example = "1")
    val id: Long,

    @field:Schema(description = "제목", example = "이전 공지사항 제목")
    val subject: String,

    @field:Schema(description = "등록일시", example = "2025.11.24")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
    val createdAt: OffsetDateTime,
)

