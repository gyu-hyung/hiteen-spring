package kr.jiasoft.hiteen.admin.services

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminSchoolClassesResponse
import kr.jiasoft.hiteen.admin.dto.AdminSchoolResponse
import kr.jiasoft.hiteen.admin.infra.AdminSchoolClassRepository
import kr.jiasoft.hiteen.admin.infra.AdminSchoolRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.PageUtil
import org.springframework.stereotype.Service

@Service
class AdminSchoolService(
    private val schools: AdminSchoolRepository,
    private val classes: AdminSchoolClassRepository,
) {
    suspend fun listSchools(
        sido: String?,
        type: Int?,
        searchType: String?,
        search: String?,
        size: Int,
        page: Int,
    ): ApiPage<AdminSchoolResponse> {
        val page = page.coerceAtLeast(1)
        val size = size.coerceIn(1, 100)
        val offset = (page - 1) * size

        // 총 레코드수
        val total = schools.countSearchResults(sido, type, searchType, search)
        val rows = schools.listSearchResults(sido, type, searchType, search, size, offset).toList()

        return PageUtil.of(items = rows, total, page, size)
    }

    suspend fun listClasses(
        schoolId: Long, year: Int
    ): List<AdminSchoolClassesResponse> {
        return classes.listBySchoolYear(schoolId, year).toList()
    }
}