package kr.jiasoft.hiteen.feature.cash.app

import kotlinx.coroutines.runBlocking
import kr.jiasoft.hiteen.feature.cash.domain.CashPolicy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class CashServiceTest {

    @Autowired
    private lateinit var cashService: CashService

    @Test
    fun `trigger` (){
        runBlocking {
            cashService.applyPolicy(1, CashPolicy.ADMIN, null, 10000)
        }
    }

}