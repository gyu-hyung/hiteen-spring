package kr.jiasoft.hiteen.admin.app

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminLevelResponse
import kr.jiasoft.hiteen.admin.infra.AdminLevelRepository
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.extensions.success
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/level")
class AdminLevelController(
    private val adminLevelRepository: AdminLevelRepository,
) {
    // 레벨 목록
    @GetMapping("/levels")
    suspend fun getLevels(
        @RequestParam searchType: String? = null,
        @RequestParam search: String? = null,
    ): ResponseEntity<ApiResult<List<AdminLevelResponse>>> {
        val data = adminLevelRepository.listLevels(search).toList()

        return success(data)
    }
}