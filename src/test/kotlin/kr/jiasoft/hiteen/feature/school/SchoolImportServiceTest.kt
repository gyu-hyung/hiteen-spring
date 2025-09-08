package kr.jiasoft.hiteen.feature.school

import kotlinx.coroutines.runBlocking
import kr.jiasoft.hiteen.feature.school.infra.SchoolRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class SchoolImportServiceTest (
    @Autowired private val schoolImportService: SchoolImportService,
    @Autowired private val schoolRepository: SchoolRepository
) {

    @Test
    fun `NEIS API에서 학교 데이터 가져오기`() = runBlocking {
        // when
//        schoolImportService.fetchAndSaveSchools()
//
//        // then
//        val count = schoolRepository.count()
//        println("저장된 학교 개수: $count")
    }


}