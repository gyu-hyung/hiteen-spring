package kr.jiasoft.hiteen.feature.play.app

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kr.jiasoft.hiteen.common.context.DeltaContextHelper
import kr.jiasoft.hiteen.feature.ad.app.AdService
import kr.jiasoft.hiteen.feature.level.app.ExpService
import kr.jiasoft.hiteen.feature.level.infra.TierRepository
import kr.jiasoft.hiteen.feature.play.domain.*
import kr.jiasoft.hiteen.feature.play.dto.FriendRankView
import kr.jiasoft.hiteen.feature.play.dto.GameScoreResponse
import kr.jiasoft.hiteen.feature.play.dto.GameStatus
import kr.jiasoft.hiteen.feature.play.dto.RankingResponse
import kr.jiasoft.hiteen.feature.play.dto.SeasonRankingResponse
import kr.jiasoft.hiteen.feature.play.dto.SeasonRoundResponse
import kr.jiasoft.hiteen.feature.play.infra.GameHistoryRepository
import kr.jiasoft.hiteen.feature.play.infra.GameRankingRepository
import kr.jiasoft.hiteen.feature.play.infra.GameRepository
import kr.jiasoft.hiteen.feature.play.infra.GameScoreRepository
import kr.jiasoft.hiteen.feature.play.infra.SeasonParticipantRepository
import kr.jiasoft.hiteen.feature.play.infra.SeasonRepository
import kr.jiasoft.hiteen.feature.point.app.PointService
import kr.jiasoft.hiteen.feature.point.domain.PointPolicy
import kr.jiasoft.hiteen.feature.push.app.event.PushSendRequestedEvent
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
import kr.jiasoft.hiteen.feature.relationship.infra.FriendRepository
import kr.jiasoft.hiteen.feature.user.domain.SeasonStatusType
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class GameService(
    private val gameRepository: GameRepository,
    private val gameScoreRepository: GameScoreRepository,
    private val gameHistoryRepository: GameHistoryRepository,
    private val gameRankingRepository: GameRankingRepository,
    private val seasonRepository: SeasonRepository,
    private val seasonParticipantRepository: SeasonParticipantRepository,
    private val tierRepository: TierRepository,
    private val rankingViewRepository: GameScoreRepository,
    private val friendRepository: FriendRepository,

    private val userRepository: UserRepository,

    private val expService: ExpService,
    private val pointService: PointService,

    private val adService: AdService,
    private val txOperator: TransactionalOperator,
    private val rewardLeagueStartNotifier: RewardLeagueStartNotifier,
    private val eventPublisher: ApplicationEventPublisher,
) {


    /**
     * 영단어 챌린지 조회
     * */
    private suspend fun getWordChallenge(): Long {
        return gameRepository.findByCode("WORD_CHALLENGE")!!.id
    }

    /**
     * 게임 목록 조회
     * */
    suspend fun getAllGames () : List<GameEntity> {
        return gameRepository.findAllByDeletedAtIsNullAndStatusOrderById("ACTIVE").toList()
    }


    /**
     * 게임 리그 조회
     * */
    suspend fun getLeague (
        user: UserEntity,
    ) : String {
        val season = seasonRepository.findActiveSeason() ?: throw NoSuchElementException("현재 진행 중인 시즌이 없습니다. 관리자 문의")
        return seasonParticipantRepository.findBySeasonIdAndUserId(season.id, user.id)?.league
            //없으면 현재 티어 기준으로 리그 반환
            ?: tierRepository.findById(user.tierId)?.let {
                calculateLeague(it.level)
            } ?: "BRONZE"
    }

    /**
     * 티어 레벨 기반 리그 계산
     * - 리그는 BRONZE, PLATINUM, CHALLENGER 3단계로 나뉨
     * - 티어 레벨 1~3 : BRONZE
     * - 티어 레벨 4~6 : PLATINUM
     * - 티어 레벨 7 이상 : CHALLENGER
     * - PLATINUM 리그 참여가능 유저가 1000명 이하인 경우 브론즈 리그로 자동참여 처리
     * - CHALLENGER 리그 참여가능 유저가 1000명 이하인 경우 플래티넘 리그로 자동참여 처리
     */
    private suspend fun calculateLeague(tierLevel: Int): String {
        return when {
            tierLevel >= 7 -> {
                val challengerCount = userRepository.countActiveChallengerUsers()
                if (challengerCount >= 1000) {
                    "CHALLENGER"
                } else {
                    val platinumCount = userRepository.countActivePlatinumUsers()
                    if (platinumCount >= 1000) "PLATINUM" else "BRONZE"
                }
            }
            tierLevel in 4..6 -> {
                val platinumCount = userRepository.countActivePlatinumUsers()
                if (platinumCount > 1000) "PLATINUM" else "BRONZE"
            }
            else -> "BRONZE"
        }
    }


    /**
     * 회차 목록 조회
     * */
    suspend fun getSeasonRounds(year: Int, status: SeasonStatusType? = null): List<SeasonRoundResponse> {
        return seasonRepository.findSeasonsByYearAndStatus(year, status?.name).toList()
    }


    /**
     * 게임상태
     * */
    suspend fun gameStatus(userId: Long, gameId: Long) : GameStatus {
        val maxTryCount = 3
        val season = seasonRepository.findActiveSeason() ?: throw NoSuchElementException("현재 진행 중인 시즌이 없습니다. 관리자 문의")
        val participant = seasonParticipantRepository.findActiveParticipant(userId, season.id)
            ?: return GameStatus(0, maxTryCount)

        val todayTryCount = gameHistoryRepository.listToday(
            gameId = gameId,
            participantId = participant.id,
            seasonId = season.id
        ).toList().size

        return GameStatus(todayTryCount, maxTryCount)
    }


    /**
     * 게임 시작
     * */
    suspend fun gameStart(
        gameId: Long,
        userId: Long,
        tierId: Long,
        retryType: String? = null,
        transactionId: String? = null
    ): UUID {
        return txOperator.executeAndAwait {
            if (!gameRepository.existsByIdAndDeletedAtIsNull(gameId))
                throw IllegalArgumentException("유효하지 않은 게임 ID 입니다. (gameId=$gameId)")

            //참가정보
            val participant = getOrCreateParticipant(userId, tierId)

            // 기존 진행중이었던 게임 확인
//            val pending =
//                gameHistoryRepository
//                .findTop1ByParticipantIdAndGameIdAndStatusOrderByCreatedAtDesc(
//                    participant.id,
//                    gameId,
//                    GameHistoryStatus.PLAYING.name
//                )

//            pending?.uid?.let { return@executeAndAwait it }

            //게임 스코어
//            val existing = gameScoreRepository.findBySeasonIdAndParticipantIdAndGameId(participant.seasonId, participant.id, gameId)
//            if (existing != null) {
//                val lastPlayedDate = existing.updatedAt?.toLocalDate() ?: existing.createdAt.toLocalDate()

                //오늘 게임한 적이 있다면 && 시도 횟수가 3회 이상이면 재도전 처리
//                if (lastPlayedDate.isEqual(LocalDate.now()) && existing.tryCount > 2) {
//                    handleRetry(gameId, userId, retryType, transactionId)
//                }
//            }

            val todayTryCount = gameHistoryRepository.listToday(gameId, participant.id, participant.seasonId,)
                .toList().size

            DeltaContextHelper.skipMeta().awaitSingleOrNull()
            if( todayTryCount >=3 ) handleRetry(gameId, userId, retryType, transactionId)



            val history = gameHistoryRepository.save(
                GameHistoryEntity(
                    seasonId = participant.seasonId,
                    participantId = participant.id,
                    gameId = gameId,
                    status = GameHistoryStatus.PLAYING.name,
                )
            )

            history.uid
        }

    }

    /**
     * 게임 종료 (점수 등록)
     * */
    suspend fun recordScore(
        gameHistoryUid: UUID,
        gameId: Long,
        score: BigDecimal,
        userId: Long,
        tierId: Long,
    ): GameScoreResponse {

        val wordChallengeGameId = getWordChallenge()

        if (!gameRepository.existsByIdAndDeletedAtIsNull(gameId))
            throw IllegalArgumentException("유효하지 않은 게임 ID 입니다. (gameId=$gameId)")

        //참가정보
        val participant = getOrCreateParticipant(userId, tierId)

        // 시도 횟수별 가산점 (0.01초 * n), 최대 1 초 까지
        val existing = gameScoreRepository.findBySeasonIdAndParticipantIdAndGameId(participant.seasonId, participant.id, gameId)
        val tryCount = (existing?.totalTryCount?.plus(1)) ?: 1
        val advantage = BigDecimal("0.01").multiply(BigDecimal(tryCount))
        val finalScore = score
            .subtract(advantage)
            .coerceAtLeast(BigDecimal.ZERO)

        //게임 이력
        saveHistory(gameHistoryUid,  participant.seasonId, participant.id, gameId, finalScore)

        //경험치 부여
        grantExp(userId, gameId, wordChallengeGameId)

        // ✅ 점수 반영 전 친구 랭킹(친구+나) 목록 저장 (추월 판정용 최소 정보)
        val friendIds = friendRepository.findAllFriendship(userId).toSet()
        val beforeRanks = getFriendRankSnapshot(
            seasonId = participant.seasonId,
            gameId = gameId,
            league = participant.league,
            userId = userId,
            friendIds = friendIds
        )

        val scoreEntity =  if (existing != null) {
            val isToday = existing.updatedAt?.toLocalDate()?.isEqual(LocalDate.now()) ?: existing.createdAt.toLocalDate().isEqual(LocalDate.now())
            updateScore(existing, finalScore, if (isToday) existing.tryCount + 1 else 1)
        } else {
            createScore(participant.seasonId, participant.id, gameId, finalScore)
        }

        // ✅ 점수 등록자 수(=game_scores) 기준으로 10명 달성 시 '보상 리그 시작' 알림 트리거
        // - recordScore(=게임 종료/점수 등록) 지점이 기준과 가장 일치해서 가장 합리적
        rewardLeagueStartNotifier.notifyIfReached(
            seasonId = participant.seasonId,
            league = participant.league,
            gameId = gameId,
        )

        // ✅ 신기록 달성 여부(개인 베스트 개선/최초 기록)
        // - 점수는 낮을수록 좋음
        val isNewRecord = (existing == null) || (scoreEntity.score < existing.score)

        // ✅ 점수 반영 후 친구 랭킹(친구+나) 목록 재조회
        // - 신기록일 때만 추월 알림을 보내기 위해, 신기록이 아니면 여기서 종료
        if (!isNewRecord) {
            return GameScoreResponse.fromEntity(scoreEntity, score, advantage)
        }

        val afterRanks = getFriendRankSnapshot(
            seasonId = participant.seasonId,
            gameId = gameId,
            league = participant.league,
            userId = userId,
            friendIds = friendIds
        )

        // ✅ "내가 특정 친구를 추월"한 경우만 해당 친구에게 푸시
        // - beforeRanks가 비어있는 경우: '이전에는 랭킹이 없었다(아예 참여/기록 X)'로 보고, after에서 친구보다 앞이면 추월로 판단
        if (afterRanks.isNotEmpty()) {
            val beforeByUser: Map<Long, FriendRankView> = beforeRanks.associateBy { it.userId }
            val afterByUser: Map<Long, FriendRankView> = afterRanks.associateBy { it.userId }

            val myAfterRank = afterByUser[userId]?.rank
            if (myAfterRank != null) {
                // 게임명
                val gameName = gameRepository.findById(gameId)?.name ?: "게임"
                // 내 닉네임
                val myNickname = userRepository.findById(userId)?.nickname ?: "친구"

                val myBeforeRank = beforeByUser[userId]?.rank ?: Int.MAX_VALUE

                // ✅ 같은 리그(=현재 랭킹 스냅샷 afterRanks)에 실제로 포함된 친구만 대상으로 필터링
                val eligibleFriendIds = friendIds.filter { afterByUser.containsKey(it) }

                val overtakenFriendIds = eligibleFriendIds.filter { friendId ->
                    val friendAfterRank = afterByUser[friendId]?.rank ?: return@filter false

                    // before에 friend가 없다면 비교 불가(친구도 랭킹에 없었다)
                    val friendBeforeRank = beforeByUser[friendId]?.rank ?: return@filter false

                    val wasBehindBefore = myBeforeRank > friendBeforeRank// 내가 친구보다 뒤였음
                    val isAheadNow = myAfterRank < friendAfterRank// 내가 친구보다 앞섰음

                    wasBehindBefore && isAheadNow
                }

                overtakenFriendIds.forEach { friendId ->
                    eventPublisher.publishEvent(
                        PushSendRequestedEvent(
                            userIds = listOf(friendId),
                            actorUserId = userId,
                            templateData = PushTemplate.GAME_OVERTAKE_FRIEND.buildPushData(
                                "nickname" to myNickname,
                                "gameName" to gameName,
                            ),
                            extraData = mapOf(
                                "gameId" to gameId.toString(),
                                "seasonId" to participant.seasonId.toString(),
                                "league" to participant.league,
                            ),
                        )
                    )
                }
            }
        }

        return GameScoreResponse.fromEntity(scoreEntity, score, advantage)
    }

    private suspend fun getFriendRankSnapshot(
        seasonId: Long,
        gameId: Long,
        league: String,
        userId: Long,
        friendIds: Set<Long>,
    ): List<FriendRankView> {
        val targetIds = (friendIds + userId)

        val participantIds = seasonParticipantRepository
            .findByUserIds(seasonId, gameId, league, targetIds)
            .map { it.id }
            .toList()
            .toTypedArray()

        if (participantIds.isEmpty()) return emptyList()

        return rankingViewRepository
            .findFriendRanks(
                seasonId = seasonId,
                gameId = gameId,
                league = league,
                participantIds = participantIds,
                limit = 200
            )
            .toList()
    }

    /**
     * 실시간 랭킹 조회 (game_scores + league 기준)
     * @param friendOnly true → 내 친구들 + 나만 랭킹 조회
     * 랭킹 기준 변경 시
     * GameScoreRepository.findScoresWithParticipantsBySeasonAndGame
     * GameScoreRepository.findSeasonRankingFiltered
     * GameScoreRepository.findFriendRanks
     *
     */
    suspend fun getRealtimeRanking(
        seasonId: Long,
        gameId: Long,
        league: String,
        currentUserId: Long,
        friendOnly: Boolean = false
    ): SeasonRankingResponse {

        // 1️⃣ friendOnly = true → season_participants.id 목록 조회
        val participantIds: Array<Long>? = if (friendOnly) {
            val friendIds = friendRepository.findAllAccepted(currentUserId)
                .map { if (it.userId == currentUserId) it.friendId else it.userId }
                .toSet()

            val targetIds = (friendIds + currentUserId) // 친구 + 나

            seasonParticipantRepository
                .findByUserIds(seasonId, gameId, league, targetIds)
                .map { it.id }
                .toList()
                .toTypedArray()
        } else null

        // 2️⃣ DB에서 friendOnly 반영해서 가져옴
        val all = rankingViewRepository.findSeasonRankingFiltered(
            seasonId = seasonId,
            gameId = gameId,
            league = league,
            participantIds = participantIds,
            limit = 100
        ).toList()

        // 3️⃣ DTO 변환
        val dtoList = all.map {
            RankingResponse(
                rank = it.rank,
                userId = it.userId,
                nickname = it.nickname,
                grade = it.grade,
                type = it.type,
                schoolName = it.schoolName,
//                modifiedSchoolName = it.schoolName,
                profileImageUrl = it.assetUid,
                score = it.score,
                tryCount = it.tryCount,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
                isMe = (it.userId == currentUserId)
            )
        }

        // 4️⃣ 기존 친구 필터링 로직은 필요 없어짐 (DB에서 이미 걸러짐)

        // 5️⃣ 내 랭킹이 Top 100에 없을 경우 → 별도 쿼리
        var myRanking = dtoList.find { it.isMe }
        if (myRanking == null) {
            val myRankEntity = rankingViewRepository.findMyRanking(seasonId, gameId, league, currentUserId)
            if (myRankEntity != null) {
                myRanking = RankingResponse(
                    rank = myRankEntity.rank,
                    userId = myRankEntity.userId,
                    nickname = myRankEntity.nickname,
                    grade = myRankEntity.grade,
                    type = myRankEntity.type,
                    schoolName = myRankEntity.schoolName,
//                    modifiedSchoolName = myRankEntity.schoolName,
                    profileImageUrl = myRankEntity.assetUid,
                    score = myRankEntity.score,
                    tryCount = myRankEntity.tryCount,
                    createdAt = myRankEntity.createdAt,
                    updatedAt = myRankEntity.updatedAt,
                    isMe = true
                )
            }
        }

        return SeasonRankingResponse(
            rankings = dtoList,
            myRanking = myRanking
        )
    }




    /**
     * 시즌 종료 후 저장된 랭킹 이력 조회 (리그 기준)
     */
    suspend fun getSeasonRanking(
        seasonId: Long,
        gameId: Long,
        league: String,
        currentUserId: Long
    ): SeasonRankingResponse {
        // 랭킹 이력 직접 조회
        val rankings = gameRankingRepository
            .findAllBySeasonIdAndGameIdAndLeague(seasonId, gameId, league)
            .toList()

        val dtoList = rankings.map {
            RankingResponse(
                rank = it.rank,
                userId = it.userId,
                nickname = it.nickname,
                grade = it.grade,
                type = it.type,
                schoolName = it.schoolName,
//                modifiedSchoolName = it.schoolName,
                profileImageUrl = it.assetUid,
                score = it.score,
                tryCount = it.tryCount,  // TODO: 시즌 기록에는 시도 횟수 없음
                createdAt = it.createdAt,
                updatedAt = it.createdAt,
                isMe = (it.userId == currentUserId)
            )
        }

        val myRanking = dtoList.find { it.isMe }

        return SeasonRankingResponse(
            rankings = dtoList,
            myRanking = myRanking
        )
    }




    /**
     * 시즌 종료 후 저장된 랭킹 이력 + 친구 필터링 (리그 기준)
     */
    suspend fun getSeasonRankingFiltered(
        seasonId: Long,
        gameId: Long,
        league: String,
        currentUserId: Long,
        allowedUserIds: List<Long>
    ): SeasonRankingResponse {
        val fullRanking = getSeasonRanking(seasonId, gameId, league, currentUserId)

        val filtered = fullRanking.rankings
            .filter { it.userId in allowedUserIds || it.isMe }
            .mapIndexed { idx, r ->
                r.copy(rank = idx + 1)
            }

        val myRanking = filtered.find { it.isMe }

        return SeasonRankingResponse(filtered, myRanking)
    }

    // ====== helpers (recordScore에서 사용하는 함수들) ======

    private suspend fun grantExp(userId: Long, gameId: Long, wordChallengeGameId: Long?) {
        if (gameId == wordChallengeGameId) {
            expService.grantExp(userId, "ENGLISH_CHALLENGE", gameId)
        } else {
            expService.grantExp(userId, "GAME_PLAY", gameId)
        }
    }

    private suspend fun getOrCreateParticipant(userId: Long, tierId: Long): SeasonParticipantEntity {
        val season = seasonRepository.findActiveSeason() ?: throw NoSuchElementException("현재 진행 중인 시즌이 없습니다. 관리자 문의")
        return seasonParticipantRepository.findActiveParticipant(userId, season.id) ?: run {
            val tier = tierRepository.findById(tierId)
                ?: throw IllegalStateException("유저의 리그를 확인할 수 없습니다.")

            val finalLeague = calculateLeague(tier.level)

            seasonParticipantRepository.save(
                SeasonParticipantEntity(
                    seasonId = season.id,
                    userId = userId,
                    tierId = tierId,
                    league = finalLeague,
                    joinedType = "AUTO_JOIN"
                )
            )
        }
    }

    private suspend fun handleRetry(
        gameId: Long,
        userId: Long,
        retryType: String?,
        transactionId: String?,
    ) {
        when (retryType) {
            "POINT" -> pointService.applyPolicy(userId, PointPolicy.GAME_PLAY, gameId)
            "AD" -> {
                if (transactionId == null) throw IllegalArgumentException("광고 재도전은 transactionId 가 필요합니다.")
                // 광고 시청 인증, 재도전(최대 5회) 보상 횟수 체크 포함
                adService.verifyAdRewardAndUseForRetry(transactionId, userId, gameId)
            }
            else -> throw IllegalStateException("오늘 이미 게임을 진행했어~")
        }
    }

    private suspend fun updateScore(existing: GameScoreEntity, score: BigDecimal, tryCount: Int? = null): GameScoreEntity {
        val finalScore = minOf(existing.score, score)
        // 기존 점수가 더 좋을 경우 updatedAt 유지
        val finalUpdatedAt = if(existing.score > score) OffsetDateTime.now() else existing.updatedAt

        val updated = existing.copy(
            score = finalScore,
            tryCount = tryCount ?: (existing.tryCount + 1),
            totalTryCount = existing.totalTryCount + 1,
            updatedAt = finalUpdatedAt
        )
        return gameScoreRepository.save(updated)
    }

    private suspend fun createScore(seasonId: Long, participantId: Long, gameId: Long, score: BigDecimal): GameScoreEntity {
        val newScore = GameScoreEntity(
            seasonId = seasonId,
            participantId = participantId,
            gameId = gameId,
            score = score,
            tryCount = 1,
            totalTryCount = 1
        )
        return gameScoreRepository.save(newScore)
    }

    private suspend fun saveHistory(gameHistoryUid: UUID, seasonId: Long, participantId: Long, gameId: Long, score: BigDecimal){
        val history = gameHistoryRepository.findByUidAndSeasonIdAndParticipantIdAndGameId(gameHistoryUid, seasonId, participantId, gameId)
            ?: throw IllegalArgumentException("유효하지 않은 게임 이력 ID 입니다. (gameHistoryUid=$gameHistoryUid)")
        //이미 종료된 게임인지 확인
        if (history.status == GameHistoryStatus.DONE.name) {
            throw IllegalStateException("이미 종료된 게임 이력입니다. (gameHistoryUid=$gameHistoryUid)")
        }
        gameHistoryRepository.save(
            history.copy(status = GameHistoryStatus.DONE.name, score = score)
        )
    }
}
