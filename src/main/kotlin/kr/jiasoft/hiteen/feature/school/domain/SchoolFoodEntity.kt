package kr.jiasoft.hiteen.feature.school.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.LocalDateTime

//TODO
@Table("tb_school_food")
data class SchoolFoodEntity(
    @Id
    val id: Long? = null,
    val schoolId: Long,
    val mealDate: LocalDate,
    val code: Int,               // 급식구분 코드
    val codeName: String?,
    val meals: String?,          // 식단정보
    val calorie: String?,
    val createdAt: LocalDateTime? = LocalDateTime.now(),
    val updatedAt: LocalDateTime? = LocalDateTime.now(),
    val deletedAt: LocalDateTime? = null
)
