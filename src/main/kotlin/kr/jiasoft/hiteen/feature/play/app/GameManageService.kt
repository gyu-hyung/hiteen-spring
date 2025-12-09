package kr.jiasoft.hiteen.feature.play.app

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.launch
import kr.jiasoft.hiteen.feature.play.domain.GameRankingEntity
import kr.jiasoft.hiteen.feature.study.domain.QuestionItemsEntity
import kr.jiasoft.hiteen.feature.play.domain.SeasonEntity
import kr.jiasoft.hiteen.feature.play.infra.*
import kr.jiasoft.hiteen.feature.study.infra.QuestionItemsRepository
import kr.jiasoft.hiteen.feature.study.infra.QuestionRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class GameManageService(
    private val seasonRepository: SeasonRepository,
    private val gameRepository: GameRepository,
    private val gameScoreRepository: GameScoreRepository,
    private val gameRankingRepository: GameRankingRepository,

    private val questionRepository: QuestionRepository,
    private val questionItemsRepository: QuestionItemsRepository
) {

    /**
     * 매일 자정 실행
     * - 시즌 종료 처리
     * - 시즌 생성
     */
    suspend fun autoManageSeasons() = run {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        closeSeasons(yesterday)   // 1. 시즌 종료 및 랭킹 저장
        createNewSeasons(today)   // 2. 새로운 시즌 생성
    }

    /**
     * 1. 종료된 시즌 처리 및 랭킹 이력 저장 TODO : Reward
     */
    suspend fun closeSeasons(endDate: LocalDate? = null) {
        val target = endDate ?: LocalDate.now()

        //TODO Flow -> Mono
        val endedSeasons = seasonRepository.findAllByEndDateOrderById(target)
        // 상태 먼저 CLOSED
        endedSeasons.collect { season ->
            coroutineScope {
                launch {
                    seasonRepository.close(season.id)
                }
                launch {
                    saveSeasonRankings(season.id)  // 랭킹 저장
                }
            }
        }
    }

    /**
     * 2. 새로운 시즌 생성 (10일 단위, 마지막주는 말일까지)
     */
    suspend fun createNewSeasons(today: LocalDate) {
        // 1️⃣ 오늘이 속한 시즌 구간 계산
        val lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth())
        val startDay = when {
            today.dayOfMonth <= 10 -> 1
            today.dayOfMonth <= 20 -> 11
            else -> 21
        }
        val startDate = today.withDayOfMonth(startDay)
        val endDate = if (startDay == 21) lastDayOfMonth else startDate.plusDays(9)

        // 2️⃣ 이미 해당 시즌이 존재하면 중복 생성 방지
        if (seasonRepository.existsByStartDate(startDate)) {
            println("⚠️ 시즌(${startDate} ~ ${endDate})은 이미 존재합니다.")
            return
        }

        // 3️⃣ 이번 달(연월) 시즌 회차 계산
        val yearMonth = today.year * 100 + today.monthValue
        val existingSeasonsThisMonth = seasonRepository.findAll()
            .filter { it.startDate.year == today.year && it.startDate.monthValue == today.monthValue }
            .toList()

        val nextRoundNo = existingSeasonsThisMonth.size + 1
        val seasonNoFormatted = "$yearMonth-$nextRoundNo"

        // 4️⃣ 시즌 생성
        val season = SeasonEntity(
            seasonNo = seasonNoFormatted, // DB 컬럼은 숫자형 유지 가능
            year = today.year,
            month = today.monthValue,
            round = nextRoundNo,
            startDate = startDate,
            endDate = endDate,
            status = "ACTIVE"
        )

        val saved = seasonRepository.save(season)
        println("✅ 새로운 시즌 생성: $seasonNoFormatted (${saved.startDate} ~ ${saved.endDate})")

        // 5️⃣ 문제 세트 생성
        generateQuestionItems(saved.id)
    }





    /**
     * 시즌 생성 시 문제 아이템 20개 랜덤 배정 (type별, 중복 방지)
     */
    suspend fun generateQuestionItems(seasonId: Long) {
        val types = listOf(1, 2, 3) // 초·중·고 타입

        for (type in types) {
            // 1️⃣ 해당 type의 문제 전체 조회
            val allQuestions = questionRepository.findByType(type).toList()
            if (allQuestions.isEmpty()) continue

            // 2️⃣ 이미 사용된 question_id 목록 조회 (중복 방지용)
            val usedQuestionIds = questionItemsRepository.findAll()
                .filter { it.seasonId == seasonId && it.type == type }
                .map { it.questionId }
                .toSet()

            // 3️⃣ 사용되지 않은 문제만 남기기
            val availableQuestions = allQuestions.filter { it.id !in usedQuestionIds }
            if (availableQuestions.size < 20) {
                println("⚠️ type=$type 에 사용 가능한 문제가 ${availableQuestions.size}개 뿐입니다.")
            }

            // 4️⃣ 무작위 20개 선택
            val selectedQuestions = availableQuestions.shuffled().take(20)

            selectedQuestions.forEach { q ->
                val correctAnswer = q.answer ?: return@forEach

                // 5️⃣ 같은 type 안에서 오답 3개 추출
                val wrongAnswers = allQuestions
                    .filter { it.answer != null && it.answer != correctAnswer }
                    .shuffled()
                    .take(3)
                    .map { it.answer!! }

                // 6️⃣ 보기 구성 (정답 + 오답)
                val options = (wrongAnswers + correctAnswer).shuffled()

                // 7️⃣ JSON 배열 문자열화
                val jsonAnswers = options.joinToString(
                    prefix = "[\"",
                    postfix = "\"]",
                    separator = "\",\""
                )

                // 8️⃣ 저장
                questionItemsRepository.save(
                    QuestionItemsEntity(
                        seasonId = seasonId,
                        type = type,
                        questionId = q.id,
                        answers = jsonAnswers
                    )
                )
            }
        }
    }


    /**
     * 시즌 종료 시 게임별 리그별 랭킹 저장
     */
    suspend fun saveSeasonRankings(seasonId: Long) {
        seasonRepository.findById(seasonId)
            ?: throw IllegalStateException("시즌 정보를 찾을 수 없습니다. (seasonId=$seasonId)")

        val games = gameRepository.findAllByDeletedAtIsNullOrderById().toList()

        for (game in games) {
            // 전체 점수 + 참가자 + 사용자 한 번에 조회
            val scores = gameScoreRepository
                .findScoresWithParticipantsBySeasonAndGame(seasonId, game.id)
                .toList()

            // 리그별 그룹화
            val groupedByLeague = scores.groupBy { it.league }

            for ((league, leagueScores) in groupedByLeague) {
                // 낮은 점수가 1등
                val sorted = leagueScores.sortedWith(compareBy({ it.score }, { it.createdAt }))

                // 순위 계산 및 저장
                sorted.forEachIndexed { index, s ->
                    val ranking = GameRankingEntity(
                        seasonId = seasonId,
                        league = league,
                        gameId = game.id,
                        rank = index + 1,
                        score = s.score,
                        participantId = s.participantId,
                        userId = s.userId,
                        nickname = s.userNickname,
                        profileImage = s.userAssetUid?.toString(),
                        createdAt = s.createdAt
                    )

                    gameRankingRepository.save(ranking)
                }
            }
        }
    }



}
