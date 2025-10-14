package kr.jiasoft.hiteen.feature.study.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "영단어 학습 시작 응답 DTO")
data class StudyStartResponse(
    @field:Schema(description = "학습 세션 UID", example = "abc123-uuid")
    val uid: String,
    @field:Schema(description = "문항 개수", example = "20")
    val questionCount: Int,
    @field:Schema(description = "문항 목록")
    val questions: List<StudyQuestionResponse>
)