package kr.jiasoft.hiteen.feature.timetable.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.timetable.dto.TimeTableRequest
import kr.jiasoft.hiteen.feature.timetable.dto.TimeTableResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/timetable")
class TimeTableController(
    private val timeTableService: TimeTableService
) {

    @Operation(summary = "시간표 조회", description = "유저별 전체 시간표 조회 (요일별 묶음)")
    @GetMapping("/{userUid}")
    suspend fun getTimeTable(
        @PathVariable userUid: String
    ): ResponseEntity<ApiResult<List<TimeTableResponse>>> {
        return ResponseEntity.ok(ApiResult.success(timeTableService.getTimeTable(userUid)))
    }

    @Operation(summary = "시간표 입력/수정", description = "특정 요일, 교시의 과목을 입력 또는 수정")
    @PostMapping("/{userUid}")
    suspend fun saveOrUpdate(
        @PathVariable userUid: String,
        @Parameter(description = "요일, 교시, 과목") request: TimeTableRequest
    ): ResponseEntity<ApiResult<Unit>> {
        timeTableService.saveOrUpdate(userUid, request)
        return ResponseEntity.ok(ApiResult.success())
    }

    @Operation(summary = "시간표 과목 삭제", description = "특정 요일, 교시의 과목을 삭제(빈칸)")
    @DeleteMapping("/{userUid}/{week}/{period}")
    suspend fun delete(
        @PathVariable userUid: String,
        @PathVariable week: Int,
        @PathVariable period: Int
    ): ResponseEntity<ApiResult<Unit>> {
        timeTableService.delete(userUid, week, period)
        return ResponseEntity.ok(ApiResult.success())
    }
}
