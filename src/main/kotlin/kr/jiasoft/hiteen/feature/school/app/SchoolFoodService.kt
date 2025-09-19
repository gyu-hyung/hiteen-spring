package kr.jiasoft.hiteen.feature.school.app

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.school.domain.SchoolFoodEntity
import kr.jiasoft.hiteen.feature.school.infra.SchoolFoodRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class SchoolFoodService(
    private val schoolFoodRepository: SchoolFoodRepository
) {

    suspend fun getMeals(
        schoolId: Long,
        type: String?,
        date: LocalDate?
    ): List<SchoolFoodEntity> {
        val now = LocalDate.now()

        val (startDate, endDate, days) = when (type) {
            "prev" -> {
                val base = date?.minusDays(3) ?: now.minusDays(6)
                Triple(base, base.plusDays(3), 3)
            }
            "next" -> {
                val base = date?.plusDays(1) ?: now.plusDays(4)
                Triple(base, base.plusDays(3), 3)
            }
            else -> { // 기본 조회 (7일)
                val base = now.minusDays(3)
                Triple(base, base.plusDays(6), 7)
            }
        }

        val rows = schoolFoodRepository.findBySchoolAndDateRange(schoolId, startDate, endDate).toList()
        val result = mutableListOf<SchoolFoodEntity>()

        for (i in 0 until days) {
            val current = startDate.plusDays(i.toLong())
            val matched = rows.filter { it.mealDate == current }
            if (matched.isNotEmpty()) {
                result.addAll(matched)
            } else {
                result.add(
                    SchoolFoodEntity(
                        schoolId = schoolId,
                        mealDate = current,
                        code = "2",
                        codeName = "중식",
                        meals = null,
                        calorie = null
                    )
                )
            }
        }

        return result
    }
}
