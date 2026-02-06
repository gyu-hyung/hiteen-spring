package kr.jiasoft.hiteen.feature.inquiry.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "문의하기 응답")
data class InquiryResponse(
    @field:Schema(description = "문의 ID", example = "1")
    val id: Long,

    @field:Schema(description = "이름", example = "홍길동")
    val name: String,

    @field:Schema(description = "전화번호", example = "010-1234-5678")
    val phone: String,

    @field:Schema(description = "이메일", example = "test@example.com")
    val email: String?,

    @field:Schema(description = "문의 내용", example = "서비스 이용 관련 문의입니다.")
    val content: String,

    @field:Schema(description = "상태 (PENDING/REPLIED/CLOSED)", example = "PENDING")
    val status: String,

    @field:Schema(description = "답변 내용")
    val replyContent: String?,

    @field:Schema(description = "답변 일시")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val replyAt: OffsetDateTime?,

    @field:Schema(description = "문의 등록 일시")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime,
)

