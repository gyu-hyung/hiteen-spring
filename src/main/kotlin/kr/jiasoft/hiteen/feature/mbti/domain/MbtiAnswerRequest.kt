package kr.jiasoft.hiteen.feature.mbti.domain

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "MBTI 답변 요청 DTO")
data class MbtiAnswerRequest(
    @field:Schema(
        description = "사용자가 응답한 질문-답변 목록",
        example = """[{"number":0,"answer":5},{"number":1,"answer":3}]"""
    )
    val answers: List<MbtiAnswer>
)

data class MbtiAnswer(
    @field:Schema(description = "질문 인덱스", example = "0")
    val number: Int,
    @field:Schema(description = "사용자 답변 (1~5 Likert scale)", example = "4")
    val answer: Int
)
