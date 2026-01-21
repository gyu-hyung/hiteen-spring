package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.OffsetDateTime
import java.util.UUID

// 리포지토리에서 직접 매핑되는 중간 DTO
// `selects`는 json/text로 받아 컨트롤러에서 파싱합니다.
data class AdminPollDetailRow(
    val id: Long,
    val question: String,
    val photo: UUID?,
    val voteCount: Int,
    val commentCount: Int,
    val likeCount: Int,
    val reportCount: Int,
    val allowComment: Int,
    val options: List<String>?,
    val startDate: String?,
    val endDate: String?,
    val status: String,
    val nickname: String,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime,
    val attachments: List<UUID>?,
    val selects: String? // JSON text
)

