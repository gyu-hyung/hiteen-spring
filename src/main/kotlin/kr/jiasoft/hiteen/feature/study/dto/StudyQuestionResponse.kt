package kr.jiasoft.hiteen.feature.study.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "영단어 학습 문제 응답 DTO")
data class StudyQuestionResponse(
    @field:Schema(description = "문항 ID", example = "159")
    val questionId: Long,
    @field:Schema(description = "문제 단어", example = "courage")
    val question: String?,
    @field:Schema(description = "발음기호", example = "[ˈkʌrɪdʒ]")
    val symbol: String?,
    @field:Schema(description = "음성 파일 경로", example = "https://cdn.site/sound/courage.mp3")
    val sound: String?,
    @field:Schema(description = "보기 목록", example = "[\"용기\", \"의학\", \"차량\", \"가방\"]")
    val options: List<String>,
    @field:Schema(description = "이미지 (있을 경우)", example = "https://cdn.site/image/courage.jpg")
    val image: String? = null
)