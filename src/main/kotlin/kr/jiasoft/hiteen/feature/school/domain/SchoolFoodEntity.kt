package kr.jiasoft.hiteen.feature.school.domain

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.LocalDateTime

//TODO
@Table("school_food")
data class SchoolFoodEntity(

    @Id
    @field:Schema(description = "급식 ID", example = "1")
    val id: Long = 0,

    @field:Schema(description = "학교 ID", example = "1")
    val schoolId: Long,

    @field:Schema(description = "급식일자", example = "2025-09-19")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val mealDate: LocalDate,

    @field:Schema(description = "식사 코드", example = "2")
    val code: String,

    @field:Schema(description = "식사명", example = "중식")
    val codeName: String,

    @field:Schema(description = "급식 메뉴", example = "된장찌개, 제육볶음, 김치")
    val meals: String? = null,

    @field:Schema(description = "칼로리 정보", example = "850kcal")
    val calorie: String? = null,

    @field:Schema(description = "생성일", example = "2025.09.19")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @field:Schema(description = "수정일시", example = "2025-09-19")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val updatedAt: LocalDateTime? = null,

    @field:Schema(description = "삭제일시", example = "2025-09-19")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val deletedAt: LocalDateTime? = null
)
