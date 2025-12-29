package kr.jiasoft.hiteen.admin.services

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminPointResponse
import kr.jiasoft.hiteen.admin.infra.AdminPointRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.PageUtil
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class AdminPointService(
    private val points: AdminPointRepository,
) {
    suspend fun listPoints(
        type: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchType: String?,
        search: String?,
        currentPage: Int,
        perPage: Int
    ): ApiPage<AdminPointResponse> {
        val page = currentPage.coerceAtLeast(1)
        val perPage = perPage.coerceIn(1, 100)
        val offset = (page - 1) * perPage

        // 총 레코드수
        val total = points.countSearchResults(type, startDate, endDate, searchType, search)
        val rows = points.listSearchResults(type, startDate, endDate, searchType, search, perPage, offset).toList()

        val data = PageUtil.of(
            items = rows,
            total = total,
            page = currentPage,
            size = perPage
        )

        return data
    }
}