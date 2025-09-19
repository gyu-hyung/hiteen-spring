package kr.jiasoft.hiteen.feature.school.app

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class SchoolAddressImportServiceTest(
    @Autowired private val service: SchoolAddressImportService
) {

    @Test
    fun `학교 주소 및 좌표 Import`() = runBlocking {
        service.importAddresses()
    }
}