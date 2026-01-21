package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import kr.jiasoft.hiteen.admin.dto.AdminPollSelectResponse
import java.time.OffsetDateTime
import java.util.UUID

data class AdminPollResponse (
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
    // 상세에서 선택지 정보를 포함하기 위한 필드 추가
    val selects: List<AdminPollSelectResponse>? = emptyList<AdminPollSelectResponse>(),
)