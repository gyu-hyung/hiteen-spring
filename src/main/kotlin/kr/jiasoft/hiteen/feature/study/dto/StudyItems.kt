package kr.jiasoft.hiteen.feature.study.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "영단어 학습 문항 목록")
data class StudyItems(
    @field:Schema(description = "문항 ID 목록")
    val question: List<Long>
)
