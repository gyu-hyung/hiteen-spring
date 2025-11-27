package kr.jiasoft.hiteen.feature.school.app

import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Test

@SpringBootTest
class SchoolFoodImportServiceTest {

    @Autowired
    private lateinit var service: SchoolFoodImportService

    @Test
    fun testImport() {
        runBlocking {
//            service.import()
        }
    }

}