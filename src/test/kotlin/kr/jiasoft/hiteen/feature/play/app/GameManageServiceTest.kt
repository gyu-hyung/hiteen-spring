package kr.jiasoft.hiteen.feature.play.app

import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime
import kotlin.test.Test


@SpringBootTest
class GameManageServiceTest  {

    @Autowired
    lateinit var gameManageService: GameManageService

    @Test
    fun `trigger`() {
        runBlocking {
//            gameManageService.autoManageSeasons()
//            gameManageService.saveSeasonRankings(5)
//            gameManageService.awards(5)
//            gameManageService.generateQuestionItems(5)
            // 2026-02-11 일 시즌 종료
//            val endDate = LocalDateTime.of(2026, 2, 10, 0, 0)
//            gameManageService.closeSeasons(endDate.toLocalDate())
        }
    }

    @Test

    fun `시즌 종료 시 랭킹 저장 테스트`() {
        runBlocking {
//            gameManageService.saveSeasonRankings(4)
        }
    }

}