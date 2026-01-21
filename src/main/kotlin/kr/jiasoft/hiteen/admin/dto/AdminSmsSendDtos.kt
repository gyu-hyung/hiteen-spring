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
     * true면 전체 회원(USER) 대상으로 발송합니다.
     * (AdminPointController.give의 receivers에 "all" 넣는 방식과 동일 목적)
     */
    val sendAll: Boolean = false,

    /**
     * 수신자 목록(01012345678 형태 권장)
     * 전체 전송을 센티넬로 처리하고 싶으면 "all"을 넣어도 됩니다.
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
