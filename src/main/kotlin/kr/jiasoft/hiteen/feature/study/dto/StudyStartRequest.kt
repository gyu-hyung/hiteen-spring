package kr.jiasoft.hiteen.feature.study.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "영단어 학습 시작 요청 DTO")
data class StudyStartRequest(
    @field:Schema(description = "시즌 ID", example = "1")
    val seasonId: Long,
    @field:Schema(description = "학습 단계 타입 (1=초등, 2=중등, 3=고등)", example = "2")
    val type: Int
)