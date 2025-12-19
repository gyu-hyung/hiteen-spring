package kr.jiasoft.hiteen.challengeRewardPolicy.app

import kr.jiasoft.hiteen.challengeRewardPolicy.dto.ChallengeRewardPolicyDeleteRequest
import kr.jiasoft.hiteen.challengeRewardPolicy.dto.ChallengeRewardPolicyRow
import kr.jiasoft.hiteen.challengeRewardPolicy.dto.ChallengeRewardPolicySaveRequest
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/challengeRewardPolicy")
class ChallengeRewardPolicyController(
    private val service: ChallengeRewardPolicyService,
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
        )

        val totalCount = service.totalCount(
            search = search,
            searchType = searchType,
            status = status,
        )

        return ResponseEntity.ok().body(ApiResult.success(PageUtil.of(list, totalCount, page, size)))
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
}
