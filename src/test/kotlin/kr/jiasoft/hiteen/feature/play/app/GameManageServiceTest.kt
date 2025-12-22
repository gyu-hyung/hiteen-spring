package kr.jiasoft.hiteen.feature.play.app

import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Test


@SpringBootTest
class GameManageServiceTest  {

    @Autowired
    lateinit var gameManageService: GameManageService

    @Test
    fun `trigger`() {
        runBlocking {
//            gameManageService.autoManageSeasons()
//            gameManageService.saveSeasonRankings(4)
//            gameManageService.awards(4)
            gameManageService.generateQuestionItems(5)
        }
    }

    @Test

    fun `시즌 종료 시 랭킹 저장 테스트`() {
        runBlocking {
//            gameManageService.saveSeasonRankings(4)
        }
    }

}