package kr.jiasoft.hiteen.feature.batch

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class BatchServiceTest {

    @Autowired
    private lateinit var batchService: BatchService

    @Test
    fun `학교 정보 import` (){
        batchService.schoolImport()
    }

    @Test
    fun `학교 급식 정보 import` (){
        batchService.schoolFoodImport()
    }

    @Test
    fun `시간표 정보 import` (){
        batchService.timeTableImport()
    }


    @Test
    fun `게임 시즌 생성` (){
        batchService.autoManageSeasons()
    }
}