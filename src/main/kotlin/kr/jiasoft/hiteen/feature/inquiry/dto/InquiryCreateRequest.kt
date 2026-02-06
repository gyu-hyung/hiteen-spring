package kr.jiasoft.hiteen.feature.inquiry.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "문의하기 등록 요청")
data class InquiryCreateRequest(
    @field:Schema(description = "이름", example = "홍길동", required = true)
    val name: String,

    @field:Schema(description = "전화번호", example = "010-1234-5678", required = true)
    val phone: String,

    @field:Schema(description = "이메일", example = "test@example.com")
    val email: String? = null,

    @field:Schema(description = "문의 내용", example = "서비스 이용 관련 문의입니다.", required = true)
    val content: String,
)
