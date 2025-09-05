package kr.jiasoft.hiteen.feature.school.infra

import kr.jiasoft.hiteen.feature.school.domain.SchoolFoodEntity
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface SchoolFoodRepository : ReactiveCrudRepository<SchoolFoodEntity, Long> {
    suspend fun findBySchoolIdAndMealDate(schoolId: Long, mealDate: LocalDate): List<SchoolFoodEntity>
}