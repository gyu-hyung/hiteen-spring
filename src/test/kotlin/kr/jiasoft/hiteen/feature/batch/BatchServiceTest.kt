package kr.jiasoft.hiteen.feature.batch

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class BatchServiceTest {

    @Autowired
    private lateinit var batchService: BatchService

    @Test
    fun `게임 시즌 생성` (){
        batchService.autoManageSeasons()
    }

}