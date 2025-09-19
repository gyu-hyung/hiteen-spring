package kr.jiasoft.hiteen.feature.school.app


import kotlinx.coroutines.flow.count
import kotlinx.coroutines.runBlocking
import kr.jiasoft.hiteen.feature.school.infra.SchoolFoodRepository
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest
class SchoolFoodImportServiceTest(
    @Autowired private val schoolFoodImportService: SchoolFoodImportService,
    @Autowired private val schoolFoodRepository: SchoolFoodRepository
) {

    @Test
    fun `NEIS API에서 급식 데이터 가져오기`() = runBlocking {
        // when: 급식 데이터를 NEIS API에서 불러와 저장
        schoolFoodImportService.import()

        // then: 오늘 날짜 기준으로 저장된 데이터가 있는지 확인
        val today = LocalDate.now()
        val foods = schoolFoodRepository.findAll() // CoroutineCrudRepository 기본 제공
        val count = foods.count { it.mealDate == today }

        println("오늘 저장된 급식 개수: $count")
        assertTrue(count > 0, "오늘 날짜 기준 급식 데이터가 1건 이상 저장되어야 함")
    }
}
