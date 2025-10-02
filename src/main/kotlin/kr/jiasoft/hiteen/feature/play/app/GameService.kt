package kr.jiasoft.hiteen.feature.play.app

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.ad.app.AdService
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
import kr.jiasoft.hiteen.feature.relationship.infra.FriendRepository
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
    suspend fun getSeasonRounds(year: Int, league: String, status: String?): List<SeasonRoundResponse> {
        return seasonRepository.findSeasonsByYearAndLeagueAndStatus(year, league.uppercase(), status?.uppercase()).toList()
    }


    /**
     * 점수 등록
     * */
    suspend fun recordScore(
        gameId: Long,
        score: Long,
        userId: Long,
        tierId: Long,
        retryType: String? = null,
        transactionId: String? = null,
    ): GameScoreEntity {
        val today = LocalDate.now()

        // 0. gameId 유효성 체크
        val isValidGame = gameRepository.existsByIdAndDeletedAtIsNull(gameId)
        if (!isValidGame) {
            throw IllegalArgumentException("유효하지 않은 게임 ID 입니다. (gameId=$gameId)")
        }

        // 1. 이번 회차에 내가 속한 참가자 정보 찾기
        var participant = seasonParticipantRepository.findActiveParticipant(userId)
        if (participant == null) {
            // 참가자 없으면 → 현재 티어 기반으로 시즌 참가 자동 등록
            val tier = tierRepository.findById(tierId)
                ?: throw IllegalStateException("유저의 리그를 확인할 수 없습니다.")

            // tierCode 앞부분만 추출 (BRONZE_STAR -> BRONZE)
            val league = tier.tierCode.substringBefore("_")

            val season = seasonRepository.findActiveSeason(league)
                ?: throw IllegalStateException("현재 진행 중인 시즌이 없습니다. (league=$tier)")

            participant = seasonParticipantRepository.save(
                SeasonParticipantEntity(
                    seasonId = season.id,
                    userId = userId,
                    tierId = tierId, // 현재 티어 ID
                    joinedType = "AUTO_JOIN"
                )
            )
        }

        // 2. 해당 참가자의 시즌 정보 가져오기
        val season = seasonRepository.findById(participant.seasonId)
            ?: throw IllegalStateException("해당 시즌 정보를 찾을 수 없습니다. (seasonId=${participant.seasonId})")

        if (season.status != "ACTIVE" || season.endDate.isBefore(LocalDate.now())) {
            throw IllegalStateException("이미 종료된 시즌입니다. (seasonId=${season.id})")
        }

        // 3. 기존 점수 여부 확인
        val existing = gameScoreRepository.findBySeasonIdAndParticipantIdAndGameId(season.id, participant.id, gameId)

        return if (existing != null) {
            val lastPlayedDate = existing.updatedAt?.toLocalDate() ?: existing.createdAt.toLocalDate()

            if (lastPlayedDate.isEqual(today)) {
                // 오늘 이미 플레이함 → 재도전 체크
                if (existing.tryCount >= 1) {
                    when (retryType) {
                        "POINT" -> {
                            try {
                                pointService.usePoints(userId, 100, "GAME_RETRY", gameId, "게임 재도전")
                            } catch (e: IllegalStateException) {
                                throw IllegalStateException("포인트가 부족하여 재도전할 수 없습니다.")
                            }
                        }
                        "AD" -> {
                            if (transactionId == null) throw IllegalArgumentException("광고 재도전은 transactionId 가 필요합니다.")
                            adService.verifyAdRewardAndUseForRetry(transactionId, userId, 100, 100, gameId = gameId)
                        }

                        else -> throw IllegalStateException("오늘 무료 도전 기회를 모두 사용했습니다.")
                    }

                }
                val updated = existing.copy(
                    score = score,
                    tryCount = existing.tryCount + 1,
                    updatedAt = OffsetDateTime.now()
                )
                gameScoreRepository.save(updated)
            } else {
                // 새로운 날짜 → tryCount 초기화
                val updated = existing.copy(
                    score = score,
                    tryCount = 1,
                    updatedAt = OffsetDateTime.now()
                )
                gameScoreRepository.save(updated)
            }

        } else {
            val newScore = GameScoreEntity(
                seasonId = season.id,
                participantId = participant.id,
                gameId = gameId,
                score = score,
                tryCount = 1
            )
            gameScoreRepository.save(newScore)
        }
    }


    /**
     * 실시간 랭킹 조회 (game_scores + league 기준)
     * @param friendOnly true → 내 친구들 랭킹만
     */
    suspend fun getRealtimeRanking(
        seasonId: Long,
        gameId: Long,
        currentUserId: Long,
        friendOnly: Boolean = false
    ): SeasonRankingResponse {

        // 전체 랭킹 조회
        val all = rankingViewRepository.findSeasonRanking(seasonId, gameId).toList()

        // DTO 변환
        val dtoList = all.map {
            RankingResponse(
                rank = it.rank,
                userId = it.userId,
                nickname = it.nickname,
                profileImageUrl = it.assetUid,
                score = it.score,
                displayTime = formatScoreRaw(it.score),
                tryCount = it.tryCount,
                isMe = (it.userId == currentUserId)
            )
        }

        // 내 친구 ID 조회
        val friendIds = if (friendOnly) {
            friendRepository.findAllAccepted(currentUserId).map {
                if (it.userId == currentUserId) it.friendId else it.userId
            }.toList()
        } else emptyList()

        // 조건에 따라 전체 or 친구 랭킹 필터링
        val filtered = if (friendOnly) {
            // 친구 + 나만 필터링
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
//        league: String,
        currentUserId: Long
    ): SeasonRankingResponse {
        // 랭킹 이력 직접 조회
        val rankings = gameRankingRepository
//            .findAllBySeasonIdAndGameIdAndLeague(seasonId, gameId, league)
            .findAllBySeasonIdAndGameId(seasonId, gameId)
            .toList()

        val dtoList = rankings.map {
            RankingResponse(
                rank = it.rank,
                userId = it.userId,
                nickname = it.nickname,
                profileImageUrl = it.profileImage,
                score = it.score,
                displayTime = formatScoreRaw(it.score),
                tryCount = 0L,//TODO
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
//        league: String,
        currentUserId: Long,
        allowedUserIds: List<Long>
    ): SeasonRankingResponse {
        val fullRanking = getSeasonRanking(seasonId, gameId, currentUserId)

        val filtered = fullRanking.rankings
            .filter { it.userId in allowedUserIds || it.isMe }
            .mapIndexed { idx, r ->
                r.copy(rank = idx + 1)
            }

        val myRanking = filtered.find { it.isMe }

        return SeasonRankingResponse(filtered, myRanking)
    }




    private fun formatScoreRaw(score: Long): String {
        val minutes = (score / 10000).toInt()        // 앞 2자리 = 분
        val seconds = ((score / 100) % 100).toInt()  // 중간 2자리 = 초
        val millis = (score % 100).toInt()           // 마지막 2자리 = 밀리초
        return String.format("%02d:%02d:%02d", minutes, seconds, millis)
    }


}
