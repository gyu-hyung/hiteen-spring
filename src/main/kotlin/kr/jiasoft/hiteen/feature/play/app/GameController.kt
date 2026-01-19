package kr.jiasoft.hiteen.feature.play.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.play.domain.GameEntity
import kr.jiasoft.hiteen.feature.play.domain.GameScoreEntity
import kr.jiasoft.hiteen.feature.play.dto.GameScoreResponse
import kr.jiasoft.hiteen.feature.play.dto.GameStartRequest
import kr.jiasoft.hiteen.feature.play.dto.GameStatus
import kr.jiasoft.hiteen.feature.play.dto.ScoreRequest
import kr.jiasoft.hiteen.feature.play.dto.SeasonRankingResponse
import kr.jiasoft.hiteen.feature.play.dto.SeasonRoundResponse
import kr.jiasoft.hiteen.feature.relationship.infra.FriendRepository
import kr.jiasoft.hiteen.feature.user.domain.SeasonStatusType
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Tag(name = "Game", description = "게임 관련 API")
@RestController
@RequestMapping("/api/games")
class GameController(
    private val gameService: GameService,
    private val friendRepository: FriendRepository
) {

    @Operation(summary = "게임 목록 조회")
    @GetMapping
    suspend fun getAllGames(): List<GameEntity> {
        return gameService.getAllGames()
    }


    @Operation(summary = "현재 참여 리그 확인")
    @GetMapping("/league")
    suspend fun getAllGames(
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<ApiResult<String>> {
        return ResponseEntity.ok(ApiResult.success(gameService.getLeague(user)))
    }

    @Operation(summary = "시즌 회차 목록 조회 (연도 + 리그 + 상태)")
    @GetMapping(value = ["/seasons/{year}", "/seasons/{year}/{status}"])
    suspend fun getSeasonsByYearAndLeague(
        @Parameter(description = "연도") @PathVariable year: Int,
//        @Parameter(description = "티어", example = "BRONZE") @PathVariable league: String,
        @Parameter(description = "ACTIVE / CLOSED / PLANNED (없으면 전체)") @PathVariable(required = false) status: SeasonStatusType?
    ): ResponseEntity<ApiResult<List<SeasonRoundResponse>>>
        = ResponseEntity.ok(ApiResult.success(gameService.getSeasonRounds(year, status)))


    @Operation(summary = "게임상태")
    @GetMapping("/status")
    suspend fun gameStatus(
        @Parameter(description = "점수 등록 요청 DTO") gameId: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<ApiResult<GameStatus>> =
        ResponseEntity.ok(ApiResult.success(gameService.gameStatus(
            user.id,
            gameId
        )))


    @Operation(summary = "게임시작")
    @PostMapping("/start")
    suspend fun start(
        @Validated @Parameter(description = "점수 등록 요청 DTO") req: GameStartRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<ApiResult<Any>> =
        ResponseEntity.ok(ApiResult.success(gameService.gameStart(
            gameId = req.gameId,
            userId = user.id,
            tierId = user.tierId,
            retryType = req.retryType,
            transactionId = req.transactionId
        )))


    @Operation(summary = "게임종료")
    @PostMapping("/end")
    suspend fun recordScore(
        @Validated @Parameter(description = "점수 등록 요청 DTO") req: ScoreRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<ApiResult<GameScoreResponse>> =
        ResponseEntity.ok(ApiResult.success(gameService.recordScore(
            gameHistoryUid = req.gameHistoryUid,
            gameId = req.gameId,
            score = req.score,
            userId = user.id,
            tierId = user.tierId,
        )))


    @Operation(summary = "실시간 랭킹 조회 (game_scores + league 기준)")
    @GetMapping("/realtime/{gameId}/{seasonId}/{league}")
    suspend fun getRealtimeRanking(
        @Parameter(description = "시즌 ID") @PathVariable seasonId: Long,
        @Parameter(description = "게임 ID") @PathVariable gameId: Long,
        @Parameter(description = "리그") @PathVariable league: String,
        @Parameter(description = "친구만 여부") @RequestParam(required = false) friendOnly: Boolean? = false,
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<ApiResult<SeasonRankingResponse>>
        = ResponseEntity.ok(ApiResult.success(gameService.getRealtimeRanking(seasonId, gameId, league, user.id, friendOnly == true)))


    @Operation(summary = "이전 랭킹 조회 ")
    @GetMapping("/history/{gameId}/{seasonId}/{league}")
    suspend fun getSeasonRanking(
        @Parameter(description = "시즌 ID") @PathVariable seasonId: Long,
        @Parameter(description = "게임 ID") @PathVariable gameId: Long,
        @Parameter(description = "리그 (예: BRONZE, SILVER, GOLD...)") @PathVariable league: String,
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<ApiResult<SeasonRankingResponse>>
    = ResponseEntity.ok(ApiResult.success(gameService.getSeasonRanking(seasonId, gameId, league, user.id)))


    @Operation(summary = "친구 랭킹 조회 (이력)")
    @GetMapping("/history/{gameId}/{seasonId}/{league}/friends")
    suspend fun getFriendRanking(
        @Parameter(description = "시즌 ID") @PathVariable seasonId: Long,
        @Parameter(description = "게임 ID") @PathVariable gameId: Long,
        @Parameter(description = "리그 (예: BRONZE, SILVER, GOLD...)") @PathVariable league: String,
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<ApiResult<SeasonRankingResponse>> {
        val friendIds = friendRepository.findAllAccepted(user.id).map {
            if (it.userId == user.id) it.friendId else it.userId
        }.toList()

        return ResponseEntity.ok(ApiResult.success(gameService.getSeasonRankingFiltered(seasonId, gameId, league, user.id, friendIds)))
    }


    //TODO 경험치 5 하루 최대 3
//    @Operation(summary = "공유하기")


//    @Operation(summary = "영어 단어 학습시작")

    //TODO 경험치 10
//    @Operation(summary = "영어 단어 학습하기")






}
