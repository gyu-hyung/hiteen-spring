package kr.jiasoft.hiteen.admin.services

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminPointResponse
import kr.jiasoft.hiteen.admin.infra.AdminPointRepository
import kr.jiasoft.hiteen.feature.point.infra.PointSummaryRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.point.domain.PointEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class AdminPointService(
    private val adminPointRepository: AdminPointRepository,
    private val pointSummaryRepository: PointSummaryRepository,
) {
    suspend fun listPoints(
        type: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchType: String?,
        search: String?,
        currentPage: Int,
        perPage: Int,
        uid: UUID?,
    ): ApiPage<AdminPointResponse> {
        val page = currentPage.coerceAtLeast(1)
        val perPage = perPage.coerceIn(1, 100)
        val offset = (page - 1) * perPage

        // 총 레코드수
        val total = adminPointRepository.countSearchResults(type, startDate, endDate, searchType, search, uid)
        val rows = adminPointRepository.listSearchResults(type, startDate, endDate, searchType, search, uid, perPage, offset).toList()

        val data = PageUtil.of(
            items = rows,
            total = total,
            page = currentPage,
            size = perPage
        )

        return data
    }

    @Transactional
    suspend fun givePoint(
        userId: Long,
        pointableType: String? = null,
        pointableId: Long? = null,
        type: String = "CREDIT",
        point: Int = 0,
        memo: String? = "-"
    ): PointEntity {
        val entity = PointEntity(
            userId = userId,
            pointableType = pointableType,
            pointableId = pointableId,
            type = type,
            point = point,
            memo = memo
        )

        // 포인트 내역 저장
        val saved = adminPointRepository.save(entity)

        // 회원 총 포인트 저장
        pointSummaryRepository.upsertAddPoint(userId, point)

        return saved
    }
}