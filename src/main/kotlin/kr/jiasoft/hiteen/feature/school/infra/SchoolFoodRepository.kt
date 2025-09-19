package kr.jiasoft.hiteen.feature.school.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.school.domain.SchoolFoodEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface SchoolFoodRepository : CoroutineCrudRepository<SchoolFoodEntity, Long> {

    @Query("""
        INSERT INTO school_food (school_id, meal_date, code, code_name, meals, calorie) 
        VALUES (:schoolId, :mealDate, :code, :codeName, :meals, :calorie)
        ON CONFLICT (school_id, meal_date, code)
        DO UPDATE SET meals = excluded.meals, calorie = excluded.calorie
    """)
    suspend fun upsert(
        schoolId: Long,
        mealDate: LocalDate,
        code: String,
        codeName: String,
        meals: String,
        calorie: String?
    )


    @Query("""
        SELECT * FROM school_food 
        WHERE school_id = :schoolId
        AND meal_date BETWEEN :startDate AND :endDate
        ORDER BY meal_date ASC, code ASC
    """)
    fun findBySchoolAndDateRange(
        schoolId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<SchoolFoodEntity>


}
