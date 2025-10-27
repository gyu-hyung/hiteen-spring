package kr.jiasoft.hiteen.feature.play.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import java.time.LocalDate

@Schema(description = "시즌 라운드 정보")
data class SeasonRoundResponse(
    @Id
    @field:Schema(description = "시즌 ID/ 회차 번호", example = "1")
    val id: Long,
    @field:Schema(description = "주차", example = "2025-1")
    val seasonNo: String,
    @field:Schema(description = "년", example = "2025")
    val year: Int,
    @field:Schema(description = "월", example = "12")
    val month: Int,
//    val roundNo: Int,   // 같은 달 안에서 1회차, 2회차 ...
    val league: String? = null,
    val status: String,
    val startDate: LocalDate,
    val endDate: LocalDate
)
