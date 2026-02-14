package kr.jiasoft.hiteen.admin.services

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminBannerResponse
import kr.jiasoft.hiteen.admin.infra.AdminBannerRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.PageUtil
import org.springframework.stereotype.Service

@Service
class AdminBannerService(
    private val adminBannerRepository: AdminBannerRepository,
) {
    // 배너 목록
    suspend fun bannerList(
        type: String?,
        status: String?,
        searchType: String?,
        search: String?,
        size: Int,
        page: Int,
    ): ApiPage<AdminBannerResponse> {
        val page = page.coerceAtLeast(1)
        val size = size.coerceIn(1, 100)
        val offset = (page - 1) * size
        val search = search?.trim()?.takeIf { it.isNotBlank() }

        val total = adminBannerRepository.countBanners(type, status, searchType, search)
        val items = adminBannerRepository.listBanners(type, status, searchType, search, size, offset).toList()

        return PageUtil.of(items, total, page, size)
    }
}