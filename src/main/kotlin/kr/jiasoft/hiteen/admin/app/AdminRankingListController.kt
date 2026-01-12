package kr.jiasoft.hiteen.admin.app

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminRankingResponse
import kr.jiasoft.hiteen.admin.infra.AdminRankingListRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * AdminPlayController 패턴을 따라 만든 랭킹 목록 조회 API
 */
@RestController
@RequestMapping("/api/admin/ranking")
@PreAuthorize("hasRole('ADMIN')")
class AdminRankingListController(
    private val repository: AdminRankingListRepository,
) {

    /**
     * 실시간 랭킹 목록 조회 (game_scores)
     */
    @GetMapping("/realtime")
    suspend fun getRealtimeRanking(
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "ASC",
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam status: String? = null,

        @RequestParam uid: UUID? = null,

        @RequestParam seasonId: Long? = null,
        @RequestParam gameId: Long? = null,
        @RequestParam league: String? = null,

        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPage<AdminRankingResponse>>> {

        val list = repository.realtimeListByPage(
            page = page,
            size = size,
            order = order,
            search = search,
            searchType = searchType,
            status = status,
            uid = uid,
            seasonId = seasonId,
            gameId = gameId,
            league = league,
        ).toList()

        val totalCount = repository.realtimeTotalCount(
            search = search,
            searchType = searchType,
            status = status,
            uid = uid,
            seasonId = seasonId,
            gameId = gameId,
            league = league,
        )

        return ResponseEntity.ok(ApiResult.success(PageUtil.of(list, totalCount, page, size)))
    }


    /**
     * 시즌 저장 랭킹 목록 조회 (game_rankings)
     */
    @GetMapping("/season")
    suspend fun getSeasonRanking(
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10,
        @RequestParam order: String = "ASC",
        @RequestParam search: String? = null,
        @RequestParam searchType: String = "ALL",
        @RequestParam status: String? = null,

        @RequestParam uid: UUID? = null,

        @RequestParam seasonId: Long? = null,
        @RequestParam gameId: Long? = null,
        @RequestParam league: String? = null,//BRONZE, PLATINUM, CHALLENGER

        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPage<AdminRankingResponse>>> {

        val list = repository.seasonListByPage(
            page = page,
            size = size,
            order = order,
            search = search,
            searchType = searchType,
            status = status,
            uid = uid,
            seasonId = seasonId,
            gameId = gameId,
            league = league,
        ).toList()

        val totalCount = repository.seasonTotalCount(
            search = search,
            searchType = searchType,
            status = status,
            uid = uid,
            seasonId = seasonId,
            gameId = gameId,
            league = league,
        )

        return ResponseEntity.ok(ApiResult.success(PageUtil.of(list, totalCount, page, size)))
    }
}

