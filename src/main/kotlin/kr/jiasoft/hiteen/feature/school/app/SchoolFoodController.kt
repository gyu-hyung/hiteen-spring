package kr.jiasoft.hiteen.feature.school.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import kr.jiasoft.hiteen.feature.school.domain.SchoolFoodEntity
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/class/meals")
class SchoolFoodController(
    private val schoolFoodService: SchoolFoodService
) {

    @Operation(summary = "급식 식단표 조회", description = "type(prev, next, 기본)과 날짜를 기준으로 급식 정보를 조회합니다.")
    @GetMapping
    suspend fun getMeals(
        @Parameter(description = "학교 ID") @RequestParam schoolId: Long,
        @Parameter(description = "조회 기준 타입(prev/next)") @RequestParam(required = false) type: String?,
        @Parameter(description = "기준 날짜") @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?
    ): ResponseEntity<List<SchoolFoodEntity>> {
        val meals = schoolFoodService.getMeals(schoolId, type, date)
        return ResponseEntity.ok(meals)
    }
}