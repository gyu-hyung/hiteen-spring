package kr.jiasoft.hiteen.admin.dto

import java.util.UUID

// 관리자용 선택지 응답 DTO
data class AdminPollSelectResponse(
    val id: Long,
    val seq: Int,
    val content: String?,
    val voteCount: Int,
    val photo: UUID? = null
)

