package kr.jiasoft.hiteen.admin.services

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminPointGiveRequest
import kr.jiasoft.hiteen.admin.dto.AdminPointResponse
import kr.jiasoft.hiteen.admin.infra.AdminPointRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.interest.domain.InterestEntity
import kr.jiasoft.hiteen.feature.interest.dto.InterestRegisterRequest
import kr.jiasoft.hiteen.feature.point.domain.PointEntity
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.OffsetDateTime

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

    suspend fun givePoint(
        request: AdminPointGiveRequest
    ): PointEntity {
        return points.save(
            PointEntity(
                type = request.type,
                point = request.point,
                memo = request.memo,
                createdAt = OffsetDateTime.now(),
                userId = 0,
                pointableType = null,
                pointableId = null,
            )
        )
    }
}