package kr.jiasoft.hiteen.feature.session.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "연속 출석 정보")
data class ConsecutiveAttendDay(
    @Schema(description = "출석일", example = "2025-10-01")
    val attendDate: LocalDate,

    @Schema(description = "출석 순서", example = "1")
    val rn: Int
)
