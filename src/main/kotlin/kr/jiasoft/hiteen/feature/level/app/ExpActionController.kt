package kr.jiasoft.hiteen.feature.level.app

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.extensions.success
import kr.jiasoft.hiteen.feature.level.dto.ExpActionResponse
import kr.jiasoft.hiteen.feature.level.infra.ExpActionRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 사용자 단(앱)에서 경험치 정의(exp_actions)를 조회하는 read-only API
 */
@RestController
@RequestMapping("/api/exp-actions")
class ExpActionController(
    private val expActionRepository: ExpActionRepository,
) {

    /**
     * 경험치 액션 정의 목록 조회
     * - enabled=true만 내려주는 게 기본(앱에서 노출 가능한 항목)
     */
    @GetMapping
    suspend fun list(
        @RequestParam(required = false, defaultValue = "true") enabled: Boolean,
    ): ResponseEntity<ApiResult<List<ExpActionResponse>>> {
        val items = expActionRepository.findAllByEnabled(enabled)
            .toList()
            .map {
                ExpActionResponse(
                    actionCode = it.actionCode,
                    description = it.description,
                    points = it.points,
                    dailyLimit = it.dailyLimit,
                )
            }

        return success(items)
    }
}

