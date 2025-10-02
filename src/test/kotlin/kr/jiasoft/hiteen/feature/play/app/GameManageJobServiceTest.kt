package kr.jiasoft.hiteen.feature.play.app

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kr.jiasoft.hiteen.feature.play.infra.GameRankingRepository
import kr.jiasoft.hiteen.feature.play.infra.SeasonRepository
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest
class GameManageJobServiceTest @Autowired constructor(
    private val gameManageJobService: GameManageJobService,
    private val seasonRepository: SeasonRepository,
    private val gameRankingRepository: GameRankingRepository
) {

    @Test
    fun `시즌 생성 테스트`() = runBlocking {
        gameManageJobService.createNewSeasons(LocalDate.now())
    }

    @Test
    fun `시즌 종료 테스트`() = runBlocking {
        gameManageJobService.closeSeasons(LocalDate.now())
    }


    @Test
    fun `시즌 종료 시 랭킹 저장 테스트`() = runBlocking {
        // given
//        val season = seasonRepository.findLatestByLeague("BRONZE")
//            ?: throw IllegalStateException("테스트할 시즌이 존재해야 합니다.")

        val seasons = seasonRepository.findActiveSeasons()
        seasons.collect { season ->
            // when
            gameManageJobService.saveSeasonRankings(season.id)
        }


        // then
        val rankings = gameRankingRepository.findAllBySeasonIdAndGameId(seasons.first().id, 1).toList()
        assertTrue(rankings.isEmpty(), "랭킹 저장이 정상적으로 이루어져야 합니다.")
    }
}
