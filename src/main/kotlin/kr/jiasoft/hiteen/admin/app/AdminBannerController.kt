package kr.jiasoft.hiteen.admin.app

import kr.jiasoft.hiteen.admin.dto.AdminBannerResponse
import kr.jiasoft.hiteen.admin.services.AdminBannerService
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.extensions.success
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/banner")
class AdminBannerController (
    private val adminBannerService: AdminBannerService,
    private val assetService: AssetService,
) {

    @GetMapping("/list")
    suspend fun getBanners(
        @RequestParam type: String? = null,
        @RequestParam status: String? = null,
        @RequestParam searchType: String? = null,
        @RequestParam search: String? = null,
        @RequestParam size: Int = 10,
        @RequestParam page: Int = 1,
    ): ResponseEntity<ApiResult<ApiPage<AdminBannerResponse>>> {
        val data = adminBannerService.bannerList(type, status, searchType, search, size, page)

        return success(data)
    }
}