package kr.jiasoft.hiteen.challengeRewardPolicy.app

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.challengeRewardPolicy.dto.ChallengeRewardPolicyDeleteRequest
import kr.jiasoft.hiteen.challengeRewardPolicy.dto.ChallengeRewardPolicyRow
import kr.jiasoft.hiteen.challengeRewardPolicy.dto.ChallengeRewardPolicySaveRequest
import kr.jiasoft.hiteen.challengeRewardPolicy.dto.ChallengeRewardPolicySingleSaveRequest
import kr.jiasoft.hiteen.challengeRewardPolicy.dto.GameSelectItem
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.play.infra.GameRepository
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/challengeRewardPolicy")
class ChallengeRewardPolicyController(
    private val service: ChallengeRewardPolicyService,
    private val gameRepository: GameRepository,
) {

    /** 목록 */
    @GetMapping
    suspend fun listByPage(
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "ASC",
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam status: String? = null,
        @RequestParam type: String? = null, // BRONZE, PLATINUM, CHALLENGER
        @RequestParam id: Long? = null,
        @RequestParam uid: String? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPage<ChallengeRewardPolicyRow>>?> {
        val list = service.listByPage(
            page = page,
            size = size,
//            order = order,
            search = search,
            searchType = searchType,
            status = status,
            type = type,
        )

        val totalCount = service.totalCount(
            search = search,
            searchType = searchType,
            status = status,
            type = type,
        )

        return ResponseEntity.ok().body(ApiResult.success(PageUtil.of(list, totalCount, page, size)))
    }

    /** 단일 저장 (모달용) */
    @PostMapping
    suspend fun saveOne(
        @RequestBody req: ChallengeRewardPolicySingleSaveRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Long>> {

        val id = service.saveOne(req, user.id)

        return ResponseEntity.ok(ApiResult.success(id))
    }


    /** 저장 (등록/수정/순서) */
    @PutMapping
    suspend fun saveAll(
        @RequestBody req: ChallengeRewardPolicySaveRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) {
        return service.saveAll(req, user.id)
    }

    /** 삭제 */
    @DeleteMapping
    suspend fun delete(
        @RequestBody req: ChallengeRewardPolicyDeleteRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) {
        service.delete(req, user.id)
    }

    /** 게임 목록 조회 (select용) */
    @GetMapping("/games")
    suspend fun searchGames(
        @RequestParam(required = false) search: String? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<List<GameSelectItem>>> {
        val games = gameRepository.findAllByDeletedAtIsNullOrderById()
            .filter { game ->
                search.isNullOrBlank() || game.name.contains(search, ignoreCase = true)
            }
            .map { GameSelectItem(id = it.id, name = it.name) }
            .toList()

        return ResponseEntity.ok(ApiResult.success(games))
    }
}
