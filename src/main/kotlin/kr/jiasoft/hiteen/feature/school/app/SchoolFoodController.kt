package kr.jiasoft.hiteen.feature.school.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.school.domain.SchoolFoodAssetEntity
import kr.jiasoft.hiteen.feature.school.domain.SchoolFoodEntity
import kr.jiasoft.hiteen.feature.school.dto.SchoolFoodImageSaveRequest
import kr.jiasoft.hiteen.feature.school.dto.SchoolFoodSaveRequest
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/class")
class SchoolFoodController(
    private val schoolFoodService: SchoolFoodService
) {

    /** 급식 식단표 조회 */
    @Operation(summary = "급식 식단표 조회", description = "type(prev, next, 기본)과 날짜를 기준으로 급식 정보를 조회합니다.")
    @GetMapping("/meals")
    suspend fun getMeals(
        @Parameter(description = "학교 ID") @RequestParam schoolId: Long,
        @Parameter(description = "조회 기준 타입(prev/next)") @RequestParam(required = false) type: String?,
        @Parameter(description = "기준 날짜") @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?
    ): ResponseEntity<List<SchoolFoodEntity>> {
        val meals = schoolFoodService.getMeals(schoolId, type, date)
        return ResponseEntity.ok(meals)
    }


    /** 급식 등록 */
    @Operation(summary = "급식 등록", description = "학교/날짜/코드 기준으로 급식을 등록하거나 수정합니다.")
    @PostMapping("/meal/save")
    suspend fun saveMeal(
        @Parameter(description = "급식 등록 요청 DTO") req: SchoolFoodSaveRequest,
    ): ResponseEntity<ApiResult<Boolean>> {
        schoolFoodService.saveMeal(req)
        return ResponseEntity.ok(ApiResult.success(true,"급식 등록 완료"))
    }



    /** 급식 사진 등록 */
    @Operation(summary = "급식 사진 등록", description = "Multipart 요청으로 급식 사진을 업로드합니다.")
    @PostMapping("/meal/image/save", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun saveImage(
        @Parameter(description = "급식 사진 등록 요청 DTO") schoolFoodImageSaveRequest: SchoolFoodImageSaveRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "첨부 사진 파일") @RequestPart("file", required = false) file: FilePart?
    ): ResponseEntity<SchoolFoodAssetEntity> {
        val saved = schoolFoodService.saveImage(
            schoolId = schoolFoodImageSaveRequest.schoolId,
            userId = user.id,
            year = schoolFoodImageSaveRequest.year,
            month = schoolFoodImageSaveRequest.month,
            file = file
        )
        return ResponseEntity.ok(saved)
    }


    /** 급식 사진 보기 */
    @Operation(summary = "급식 사진 보기")
    @GetMapping("/meal/image/view")
    suspend fun viewImage(
        @RequestParam schoolId: Long,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): ResponseEntity<SchoolFoodAssetEntity?> {
        val data = schoolFoodService.viewImage(schoolId, year, month)
        return ResponseEntity.ok(data)
    }

    /** 급식 사진 신고 */
    @Operation(summary = "급식 사진 신고")
    @PostMapping("/meal/image/report")
    suspend fun reportImage(
        @RequestParam id: Long
    ): ResponseEntity<String> {
        schoolFoodService.reportImage(id)
        return ResponseEntity.ok("신고 완료")
    }
}
