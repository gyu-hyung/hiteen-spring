package kr.jiasoft.hiteen.feature.school.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "급식 등록/수정 요청 DTO")
data class SchoolFoodSaveRequest(

    @param:Schema(description = "학교 ID", example = "12345")
    val schoolId: Long,
    @param:Schema(description = "급식 날짜", example = "2025-12-01")
    val mealDate: LocalDate,
    @param:Schema(description = "급식 코드", example = "2")
    val code: String,
    @param:Schema(description = "급식 코드명", example = "중식")
    val codeName: String,
    @param:Schema(description = "급식 식단", example = "밥\n 국\n 김치")
    val meals: String?,
    @param:Schema(description = "칼로리", example = "100")
    val calorie: String?
)


@Schema(description = "급식 이미지 등록/수정 요청 DTO")
data class SchoolFoodImageSaveRequest(
    @param:Schema(description = "학교 ID", example = "12345")
    val schoolId: Long,
    @param:Schema(description = "년도", example = "2025")
    val year: Int,
    @param:Schema(description = "월", example = "12")
    val month: Int
)





