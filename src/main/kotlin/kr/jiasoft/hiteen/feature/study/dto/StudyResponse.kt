package kr.jiasoft.hiteen.feature.study.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "영단어 학습 응답 DTO")
data class StudyResponse(
    @JsonIgnore
    @field:Schema(description = "학습 ID", example = "1")
    val id: Long,
    @field:Schema(description = "학습 고유 UID", example = "abc123")
    val uid: String?,
    @field:Schema(description = "유저 ID", example = "5")
    val userId: Long,
    @field:Schema(description = "시즌 ID", example = "1")
    val seasonId: Long,
    @field:Schema(description = "문항 수", example = "20")
    val questionCount: Int,
    @field:Schema(description = "획득 포인트", example = "10")
    val givePoint: Long,
    @field:Schema(description = "상태", example = "1")
    val status: Long,
    @field:Schema(description = "학습 완료일")
    val completeDate: OffsetDateTime?
)
