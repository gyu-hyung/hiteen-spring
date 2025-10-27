package kr.jiasoft.hiteen.feature.play.app

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.ad.app.AdService
import kr.jiasoft.hiteen.feature.level.app.ExpService
import kr.jiasoft.hiteen.feature.level.infra.TierRepository
import kr.jiasoft.hiteen.feature.play.domain.*
import kr.jiasoft.hiteen.feature.play.dto.RankingResponse
import kr.jiasoft.hiteen.feature.play.dto.SeasonRankingResponse
import kr.jiasoft.hiteen.feature.play.dto.SeasonRoundResponse
import kr.jiasoft.hiteen.feature.play.infra.GameRankingRepository
import kr.jiasoft.hiteen.feature.play.infra.GameRepository
import kr.jiasoft.hiteen.feature.play.infra.GameScoreRepository
import kr.jiasoft.hiteen.feature.play.infra.SeasonParticipantRepository
import kr.jiasoft.hiteen.feature.play.infra.SeasonRepository
import kr.jiasoft.hiteen.feature.point.app.PointService
import kr.jiasoft.hiteen.feature.point.domain.PointPolicy
import kr.jiasoft.hiteen.feature.relationship.infra.FriendRepository
import kr.jiasoft.hiteen.feature.user.domain.SeasonStatusType
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime

@Service
class GameService(
    private val gameRepository: GameRepository,
    private val gameScoreRepository: GameScoreRepository,
    private val gameRankingRepository: GameRankingRepository,
    private val seasonRepository: SeasonRepository,
    private val seasonParticipantRepository: SeasonParticipantRepository,
    private val tierRepository: TierRepository,
    private val rankingViewRepository: GameScoreRepository,
    private val friendRepository: FriendRepository,

    private val expService: ExpService,
    private val pointService: PointService,

    private val adService: AdService,

) {


    /**
     * 게임 목록 조회
     * */
    suspend fun getAllGames () : List<GameEntity> {
        return gameRepository.findAll().toList()
    }

    /**
     * 회차 목록 조회
     * */
    suspend fun getSeasonRounds(year: Int, status: SeasonStatusType? = null): List<SeasonRoundResponse> {
        return seasonRepository.findSeasonsByYearAndLeagueAndStatus(year, status?.name).toList()
    }


    /**
     * 점수 등록
     * */
    suspend fun recordScore(
        gameId: Long,
        score: Double,
        userId: Long,
        tierId: Long,
        retryType: String? = null,
        transactionId: String? = null
    ): GameScoreEntity {
        val today = LocalDate.now()
        val wordChallengeGameId = gameRepository.findByCode("WORD_CHALLENGE")?.id

        if (!gameRepository.existsByIdAndDeletedAtIsNull(gameId)) {
            throw IllegalArgumentException("유효하지 않은 게임 ID 입니다. (gameId=$gameId)")
        }

        //참가정보
        val participant = getOrCreateParticipant(userId, tierId)

        //게임 이력
        val existing = gameScoreRepository.findBySeasonIdAndParticipantIdAndGameId(participant.seasonId, participant.id, gameId)

        return if (existing != null) {
            val lastPlayedDate = existing.updatedAt?.toLocalDate() ?: existing.createdAt.toLocalDate()

            if (lastPlayedDate.isEqual(today)) {
                handleRetry(gameId, userId, retryType, transactionId)
                grantExp(userId, gameId, wordChallengeGameId)

                // 시도 횟수별 가산점 (0.08초 * n)
                val nextTryCount = existing.tryCount + 1
                val advantage = 0.08 * (nextTryCount - 1)

                // 0.08초 단위는 Double 이므로 Long score 변환 시 ms 단위로 보정 필요할 수 있음
                val adjustedScore = (score - advantage).coerceAtLeast(0.0)

                saveOrUpdateScore(existing, adjustedScore, nextTryCount)
            } else {

                pointService.applyPolicy(userId, PointPolicy.GAME_PLAY, gameId)
                grantExp(userId, gameId, wordChallengeGameId)
                saveOrUpdateScore(existing, score, 1)
            }
        } else {

            pointService.applyPolicy(userId, PointPolicy.GAME_PLAY, gameId)
            grantExp(userId, gameId, wordChallengeGameId)
            createNewScore(participant.seasonId, participant.id, gameId, score)
        }
    }

    private suspend fun grantExp(userId: Long, gameId: Long, wordChallengeGameId: Long?) {
        if (gameId == wordChallengeGameId) {
            expService.grantExp(userId, "GAME_PLAY", gameId, 15)
        } else {
            expService.grantExp(userId, "GAME_PLAY", gameId)
        }
    }


    // =============== 헬퍼 메서드 ===============

    private suspend fun getOrCreateParticipant(userId: Long, tierId: Long): SeasonParticipantEntity {
        return seasonParticipantRepository.findActiveParticipant(userId) ?: run {
            val tier = tierRepository.findById(tierId)
                ?: throw IllegalStateException("유저의 리그를 확인할 수 없습니다.")
            val season = seasonRepository.findActiveSeason()
                ?: throw NoSuchElementException("현재 진행 중인 시즌이 없습니다. 관리자 문의")
            if (season.status != SeasonStatusType.ACTIVE.name || season.endDate.isBefore(LocalDate.now())) {
                throw IllegalStateException("이미 종료된 시즌입니다. (seasonId=${season.id})")
            }
            seasonParticipantRepository.save(
                SeasonParticipantEntity(
                    seasonId = season.id,
                    userId = userId,
                    tierId = tierId,
                    league = tier.tierCode.split("_")[0],
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

    private suspend fun saveOrUpdateScore(existing: GameScoreEntity, score: Double, tryCount: Int): GameScoreEntity {
        val finalScore = maxOf(existing.score, score)
        val updated = existing.copy(
            score = finalScore,
            tryCount = tryCount,
            updatedAt = OffsetDateTime.now()
        )
        return gameScoreRepository.save(updated)
    }

    private suspend fun createNewScore(seasonId: Long, participantId: Long, gameId: Long, score: Double): GameScoreEntity {
        val newScore = GameScoreEntity(
            seasonId = seasonId,
            participantId = participantId,
            gameId = gameId,
            score = score,
            tryCount = 1
        )
        return gameScoreRepository.save(newScore)
    }



    /**
     * 실시간 랭킹 조회 (game_scores + league 기준)
     * @param friendOnly true → 내 친구들 랭킹만
     */
    suspend fun getRealtimeRanking(
        seasonId: Long,
        gameId: Long,
        league: String,
        currentUserId: Long,
        friendOnly: Boolean = false
    ): SeasonRankingResponse {

        // 전체 랭킹 조회
        val all = rankingViewRepository.findSeasonRanking(seasonId, gameId, league).toList()

        // DTO 변환 (displayTime은 DTO 내부에서 자동 계산됨)
        val dtoList = all.map {
            RankingResponse(
                rank = it.rank,
                userId = it.userId,
                nickname = it.nickname,
                profileImageUrl = it.assetUid,
                score = it.score,
                tryCount = it.tryCount,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
                isMe = (it.userId == currentUserId)
            )
        }

        // 내 친구 ID 조회
        val friendIds = if (friendOnly) {
            friendRepository.findAllAccepted(currentUserId)
                .map { if (it.userId == currentUserId) it.friendId else it.userId }
                .toList()
        } else emptyList()

        // 조건에 따라 전체 or 친구 랭킹 필터링
        val filtered = if (friendOnly) {
            val onlyFriends = dtoList.filter { it.userId in friendIds || it.isMe }
            // 순위를 1부터 다시 매기기
            onlyFriends.mapIndexed { index, r ->
                r.copy(rank = index + 1)
            }
        } else {
            dtoList
        }

        val myRanking = filtered.find { it.isMe }

        return SeasonRankingResponse(
            rankings = filtered,
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
                profileImageUrl = it.profileImage,
                score = it.score,
                tryCount = 0,  // TODO: 시즌 기록에는 시도 횟수 없음
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




}
