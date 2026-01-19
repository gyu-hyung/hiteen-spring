package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

@Schema(description = "관리자 SMS 발송 요청")
data class AdminSmsSendRequest(
    @field:Size(max = 255)
    val title: String? = null,

    @field:NotBlank
    @field:Size(max = 255)
    val content: String,

    /**
     * sender 전화번호(발신번호). 비어있으면 application.yml의 sms.callback 사용
     */
    @field:Size(max = 30)
    val callback: String? = null,

    /**
     * 수신자 목록(01012345678 형태 권장)
     */
    val phones: List<@NotBlank String>,
)

@Schema(description = "관리자 SMS 발송 결과")
data class AdminSmsSendResponse(
    val smsId: Long,
    val total: Long,
    val success: Long,
    val failure: Long,
    val failedPhones: List<String> = emptyList(),

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime,
)

