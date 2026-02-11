package kr.jiasoft.hiteen.admin.services

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminCashResponse
import kr.jiasoft.hiteen.admin.infra.AdminCashRepository
import kr.jiasoft.hiteen.feature.cash.infra.CashSummaryRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.cash.domain.CashEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class AdminCashService(
    private val adminCashRepository: AdminCashRepository,
    private val cashSummaryRepository: CashSummaryRepository,
) {
    suspend fun listCash(
        type: String?,
        status: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchType: String?,
        search: String?,
        page: Int,
        size: Int,
        uid: UUID?,
    ): ApiPage<AdminCashResponse> {
        val page = page.coerceAtLeast(1)
        val size = size.coerceIn(1, 100)
        val offset = (page - 1) * size

        // 총 레코드수
        val total = adminCashRepository.countSearchResults(type, status, startDate, endDate, searchType, search, uid)
        val rows = adminCashRepository.listSearchResults(type, status, startDate, endDate, searchType, search, uid, size, offset).toList()

        return PageUtil.of(items = rows, total, page, size)
    }

    @Transactional
    suspend fun giveCash(
        userId: Long,
        cashableType: String? = null,
        cashableId: Long? = null,
        type: String = "CREDIT",
        amount: Int = 0,
        memo: String? = "-"
    ): CashEntity {
        val entity = CashEntity(
            userId = userId,
            cashableType = cashableType,
            cashableId = cashableId,
            type = type,
            amount = amount,
            memo = memo
        )

        // 캐시 내역 저장
        val saved = adminCashRepository.save(entity)

        // 회원 총 캐시 저장
        cashSummaryRepository.upsertAddPoint(userId, amount)

        return saved
    }
}

