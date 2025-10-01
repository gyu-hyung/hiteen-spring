package kr.jiasoft.hiteen.feature.play.app

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.play.domain.GameRankingEntity
import kr.jiasoft.hiteen.feature.play.domain.League
import kr.jiasoft.hiteen.feature.play.domain.SeasonEntity
import kr.jiasoft.hiteen.feature.play.infra.*
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class GameManageJobService(
    private val seasonRepository: SeasonRepository,
    private val gameRepository: GameRepository,
    private val gameScoreRepository: GameScoreRepository,
    private val seasonParticipantRepository: SeasonParticipantRepository,
    private val gameRankingRepository: GameRankingRepository,
    private val userRepository: UserRepository
) {

    /**
     * 매일 자정 실행
     * - 시즌 종료 처리
     * - 시즌 생성
     */
    @Scheduled(cron = "0 0 0 * * *")
    suspend fun autoManageSeasons() {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        closeSeasons(yesterday)   // 1. 시즌 종료 및 랭킹 저장
        createNewSeasons(today)   // 2. 새로운 시즌 생성
    }

    /**
     * 1. 종료된 시즌 처리 및 랭킹 이력 저장
     */
    suspend fun closeSeasons(endDate: LocalDate) {
        val endedSeasons = seasonRepository.findAllByEndDate(endDate)
        endedSeasons.collect { season ->
            saveSeasonRankings(season.id)  // 랭킹 저장
            seasonRepository.close(season.id) // 상태 CLOSED
        }
    }

    /**
     * 2. 새로운 시즌 생성
     */
    suspend fun createNewSeasons(startDate: LocalDate) {
        val exists = seasonRepository.existsByStartDate(startDate)
        if (exists) return

        League.entries.forEach { league ->
            val latest = seasonRepository.findLatestByLeague(league.name)
            val newSeasonNo = (latest?.seasonNo ?: 0) + 1

            val season = SeasonEntity(
                seasonNo = newSeasonNo,
                league = league.name,
                startDate = startDate,
                endDate = startDate.plusDays(9), // 10일간 유지
                status = "ACTIVE"
            )
            seasonRepository.save(season)
        }
    }

    /**
     * 3. 시즌 종료 시 게임별 랭킹 저장
     */
    suspend fun saveSeasonRankings(seasonId: Long) {
        val season = seasonRepository.findById(seasonId)
            ?: throw IllegalStateException("시즌 정보를 찾을 수 없습니다. (seasonId=$seasonId)")

        val games = gameRepository.findAllByDeletedAtIsNull().toList()

        for (game in games) {
            val scoresFlow = gameScoreRepository.findBySeasonIdAndGameIdOrderByScoreAsc(seasonId, game.id)

            scoresFlow.collectIndexed { idx, score ->
                val participant = seasonParticipantRepository.findById(score.participantId) ?: return@collectIndexed
                val user = userRepository.findById(participant.userId) ?: return@collectIndexed

                gameRankingRepository.save(
                    GameRankingEntity(
                        seasonId = seasonId,
                        league = season.league,
                        participantId = participant.id,
                        gameId = game.id,
                        userId = user.id,
                        rank = idx + 1,
                        score = score.score,
                        nickname = user.nickname,
                        profileImage = user.assetUid?.toString()
                    )
                )
            }
        }
    }
}
