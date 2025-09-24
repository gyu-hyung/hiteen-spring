package kr.jiasoft.hiteen.feature.timetable.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "시간표 응답 DTO")
data class TimeTableSlotResponse(
    @param:Schema(description = "요일", example = "1")
    val week: Int,
    @param:Schema(description = "교시", example = "1")
    val period: Int,
    @param:Schema(description = "과목", example = "수학")
    val subject: String?
)

@Schema(description = "시간표 응답 DTO")
data class TimeTableResponse(
    @param:Schema(description = "요일", example = "1")
    val week: Int,
    @param:Schema(description = "교시")
    val slots: List<TimeTableSlotResponse>
)

@Schema(description = "시간표 등록 요청 DTO")
data class TimeTableRequest(

    @param:Schema(description = "요일", example = "1")
    val week: Int,
    @param:Schema(description = "교시", example = "1")
    val period: Int,
    @param:Schema(description = "과목", example = "수학")
    val subject: String?
)
