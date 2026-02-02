package kr.jiasoft.hiteen.feature.play.app

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.launch
import kr.jiasoft.hiteen.challengeRewardPolicy.domain.ChallengeRewardPolicyEntity
import kr.jiasoft.hiteen.challengeRewardPolicy.infra.ChallengeRewardPolicyRepository
import kr.jiasoft.hiteen.feature.cash.app.CashService
import kr.jiasoft.hiteen.feature.cash.domain.CashPolicy
import kr.jiasoft.hiteen.feature.gift.app.GiftAppService
import kr.jiasoft.hiteen.feature.gift.domain.GiftCategory
import kr.jiasoft.hiteen.feature.gift.domain.GiftType
import kr.jiasoft.hiteen.feature.gift.dto.GiftProvideRequest
import kr.jiasoft.hiteen.feature.play.domain.GameRankingEntity
import kr.jiasoft.hiteen.feature.study.domain.QuestionItemsEntity
import kr.jiasoft.hiteen.feature.play.domain.SeasonEntity
import kr.jiasoft.hiteen.feature.play.dto.RankingRow
import kr.jiasoft.hiteen.feature.play.infra.*
import kr.jiasoft.hiteen.feature.study.infra.QuestionItemsRepository
import kr.jiasoft.hiteen.feature.study.infra.QuestionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class GameManageService(
    private val seasonRepository: SeasonRepository,
    private val gameRepository: GameRepository,
    private val gameScoreRepository: GameScoreRepository,
    private val gameRankingRepository: GameRankingRepository,

    private val questionRepository: QuestionRepository,
    private val questionItemsRepository: QuestionItemsRepository,

    //랭킹 보상
    private val giftAppService: GiftAppService,
    private val challengeRewardPolicyRepository: ChallengeRewardPolicyRepository,
    private val cashService: CashService,

    ) {


    private val log = LoggerFactory.getLogger(GameManageService::class.java)


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


    private fun calculateSeasonRange(today: LocalDate): Pair<LocalDate, LocalDate> {
        val lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth())

        val startDay = when {
            today.dayOfMonth <= 10 -> 1
            today.dayOfMonth <= 20 -> 11
            else -> 21
        }

        val startDate = today.withDayOfMonth(startDay)
        val endDate = if (startDay == 21) lastDayOfMonth else startDate.plusDays(9)

        return startDate to endDate
    }



    /**
     * 1. 종료된 시즌 처리 및 랭킹 이력 저장
     */
    suspend fun closeSeasons(today: LocalDate = LocalDate.now()) {

        // 1️⃣ 오늘이 속한 시즌 구간 계산
        val (startDate, endDate) = calculateSeasonRange(today)

        // 2️⃣ 오늘이 시즌 종료일이 아니면 아무 것도 하지 않음
        if (today != endDate) {
            log.info("ℹ️ 오늘($today)은 시즌 종료일($endDate)이 아닙니다. 종료 처리 스킵")
            return
        }

        // 3️⃣ 종료일이 today 인 ACTIVE 시즌만 종료 처리
        val seasonsToClose = seasonRepository
            .findAllByEndDateOrderById(today)
            .filter { it.status == "ACTIVE" }
            .toList()

        if (seasonsToClose.isEmpty()) {
            log.info("ℹ️ 종료일이 $today 인 ACTIVE 시즌이 없습니다. ($startDate ~ $endDate)")
            return
        }

        // 4️⃣ 시즌 종료 처리 (순차 실행: close → saveRankings → awards)
        seasonsToClose.forEach { season ->
//            seasonRepository.close(season.id)
//            saveSeasonRankings(season.id)
//            awards(season.id)

            log.info("🏁 시즌 종료 처리 완료: {} ({} ~ {})", season.seasonNo, season.startDate, season.endDate)
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
                //sound, image 값 있는것만 filter
                .filter { !it.sound.isNullOrBlank() && !it.image.isNullOrBlank()}

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
                // 낮은 점수가 1등 updatedAt가 있으면 더 이전 시간 순으로 정렬
                val sorted = leagueScores.sortedWith(
                    compareBy(
                        { it.score },
                        { it.updatedAt ?: it.createdAt }
                    )
                )


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



    suspend fun awards(seasonId: Long) = coroutineScope {

        // 1️⃣ 시즌 랭킹 전체 조회
        val rankings = gameRankingRepository
            .findBySeasonId(seasonId)
            .toList()

        if (rankings.isEmpty()) return@coroutineScope

        // 2️⃣ 정책 전체 조회 (ACTIVE)
        val policies = challengeRewardPolicyRepository
            .findAll()
            .filter { it.status.toInt() == 1 && it.deletedAt == null }
            .toList()

        // 3️⃣ (리그 + 게임) 기준으로 그룹화
        val groupedByLeagueGame = rankings.groupBy {
            it.league to it.gameId
        }

        // 4️⃣ 그룹 단위로 처리
        groupedByLeagueGame.forEach { (key, groupRankings) ->
            val (league, gameId) = key

            val participantCount = groupRankings.size

            // 🚫 인원 수 부족 → 전체 스킵
            if (participantCount < 10) {
                log.info(
                    "Reward SKIPPED - season=$seasonId league=$league game=$gameId (count=$participantCount)"
                )
                return@forEach
            }

            // 5️⃣ 랭킹 단위 정책 매칭
            groupRankings.forEach { ranking ->
                val matchedPolicies = policies.filter { policy ->
                    policy.league == league &&
                            policy.rank == ranking.rank &&
                            (policy.gameId == null || policy.gameId == gameId)
                }

                matchedPolicies.forEach { policy ->
                    launch {
                        giveReward(
                            policy = policy,
                            ranking = ranking,
                            seasonId = seasonId
                        )
                    }
                }
            }
        }
    }






    private suspend fun giveReward(
        policy: ChallengeRewardPolicyEntity,
        ranking: RankingRow,
        seasonId: Long
    ) {
        when (policy.type) {

            "CASH" -> {
                cashService.applyPolicy(
                    userId = ranking.userId,
                    cashPolicy = CashPolicy.CHALLENGE_REWARD,
                    refId = seasonId,
                    dynamicCash = policy.amount,
                    description = "[하이틴] 챌린지 ${ranking.rank}등 보상 도착!"
                )
            }

            "GIFTISHOW", "GIFT_CARD" -> {
                val goodsCodes = policy.goodsCodes
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?: return

                goodsCodes.forEach { goodsCode ->
                    giftAppService.createGift(
                        ranking.userId,
                        GiftProvideRequest(
                            giftType =
                                if (goodsCode.startsWith("G"))
                                    GiftType.Voucher
                                else
                                    GiftType.GiftCard,

                            giftCategory = GiftCategory.Challenge,
                            receiveUserUids = listOf(ranking.userUid!!),
                            memo = policy.message,

                            goodsCode = goodsCode,
                            gameId = ranking.gameId,
                            seasonId = seasonId,
                            seasonRank = ranking.rank
                        )
                    )
                }
            }

        }
    }




}
