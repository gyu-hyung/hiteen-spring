package kr.jiasoft.hiteen.admin.services

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminExpResponse
import kr.jiasoft.hiteen.admin.infra.AdminExpRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.PageUtil
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class AdminExpService(
    private val adminExpRepository: AdminExpRepository,
) {
    // 경험치 내역
    suspend fun expHistory(
        status: String?,
        type: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchType: String?,
        search: String?,
        uid: UUID?,
        page: Int,
        perPage: Int,
    ): ApiPage<AdminExpResponse> {
        val page = page.coerceAtLeast(1)
        val perPage = perPage.coerceIn(1, 100)
        val offset = (page - 1) * perPage
        val search = search?.trim()?.takeIf { it.isNotBlank() }

        // 총 레코드수
        val total = adminExpRepository.countSearch(status, type, startDate, endDate, searchType, search, uid)
        val rows = adminExpRepository.listSearch(status, type, startDate, endDate, searchType, search, uid, perPage, offset).toList()

        return PageUtil.of(
            items = rows,
            total = total,
            page = page,
            size = perPage,
        )
    }
}