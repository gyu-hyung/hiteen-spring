package kr.jiasoft.hiteen.admin.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "관리자 신고 반려 요청")
data class AdminReportRejectRequest(
    @field:Schema(description = "반려 사유/메모")
    val memo: String? = null,
)

