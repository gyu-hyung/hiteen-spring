package kr.jiasoft.hiteen.admin.services

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminSchoolResponse
import kr.jiasoft.hiteen.admin.infra.AdminSchoolRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.PageUtil
import org.springframework.stereotype.Component

@Component
class AdminSchoolService(
    private val schools: AdminSchoolRepository,
) {
    suspend fun listSchools(
        sido: String?, type: Int?, searchType: String?, search: String?,
        currentPage: Int, perPage: Int
    ): ApiPage<AdminSchoolResponse> {
        val page = currentPage.coerceAtLeast(1)
        val perPage = perPage.coerceIn(1, 100)
        val offset = (page - 1) * perPage

        // 총 레코드수
        val total = schools.countSearchResults(sido, type, searchType, search)
        val rows = schools.listSearchResults(sido, type, searchType, search, perPage, offset).toList()

        val data = PageUtil.of(
            items = rows,
            total = total,
            page = currentPage,
            size = perPage
        )

        return data
    }
}