package kr.jiasoft.hiteen.feature.poll.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "투표 템플릿 응답 DTO")
data class PollTemplateResponse(
    val id: Long,
    val question: String,
    val answers: List<String>,
    val state: Short,
)

@Schema(description = "투표 템플릿 생성/수정 DTO")
data class PollTemplateRequest(
    val question: String,
    val answers: List<String>,
    val state: Short = 1
)
