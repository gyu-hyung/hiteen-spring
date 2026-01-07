package kr.jiasoft.hiteen.feature.school.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.common.dto.ApiPageCursor
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.school.dto.SchoolClassResponse
import kr.jiasoft.hiteen.feature.school.dto.SchoolGradeResponse
import kr.jiasoft.hiteen.feature.school.dto.SchoolDto
import kr.jiasoft.hiteen.feature.school.infra.SchoolClassesRepository
import kr.jiasoft.hiteen.feature.school.infra.SchoolRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@Tag(name = "School", description = "학교 정보 조회 API")
@RestController
@RequestMapping("/api/school")
class SchoolController(
    private val schoolRepository: SchoolRepository,
    private val schoolClassesRepository: SchoolClassesRepository,
) {


    // ============================================================
    // ✅ 1. schoolId → 학년 목록 조회
    // ============================================================
    @Operation(summary = "학교 학년 목록 조회")
    @GetMapping("/{schoolId}/grades")
    suspend fun getGrades(
        @PathVariable schoolId: Long
    ): ResponseEntity<ApiResult<List<SchoolGradeResponse>>> {

        val grades = schoolClassesRepository
            .findGradesBySchoolId(schoolId)
            .map { SchoolGradeResponse(it) }
            .toList()

        return ResponseEntity.ok(ApiResult.success(grades))
    }

    // ============================================================
    // ✅ 2. schoolId + grade → 학급 목록 조회
    // ============================================================
    @Operation(summary = "학교 학급 목록 조회")
    @GetMapping("/{schoolId}/grades/{grade}/classes")
    suspend fun getClasses(
        @PathVariable schoolId: Long,
        @PathVariable grade: String
    ): ResponseEntity<ApiResult<List<SchoolClassResponse>>> {

        val classes = schoolClassesRepository
            .findBySchoolIdAndGrade(schoolId, grade)
            .map {
                SchoolClassResponse(
                    id = it.id,
                    classNo = it.classNo,
                    className = it.className
                )
            }
            .toList()

        return ResponseEntity.ok(ApiResult.success(classes))
    }
}
