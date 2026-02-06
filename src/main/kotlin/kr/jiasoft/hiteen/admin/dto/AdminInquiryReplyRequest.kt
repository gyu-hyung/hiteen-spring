package kr.jiasoft.hiteen.admin.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "문의 답변 SMS 발송 요청")
data class AdminInquiryReplyRequest(
    @field:Schema(description = "답변 내용", example = "문의해 주신 내용에 대해 답변드립니다...", required = true)
    val replyContent: String,

    @field:Schema(description = "SMS 발송 여부", example = "true")
    val sendSms: Boolean = true,

    @field:Schema(description = "SMS 메시지 (미입력시 답변 내용 사용)")
    val smsMessage: String? = null,
)

