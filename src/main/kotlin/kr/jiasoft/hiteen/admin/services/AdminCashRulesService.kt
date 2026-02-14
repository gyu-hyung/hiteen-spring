package kr.jiasoft.hiteen.admin.services

import kr.jiasoft.hiteen.admin.dto.AdminCashRuleCreateRequest
import kr.jiasoft.hiteen.admin.dto.AdminCashRuleResponse
import kr.jiasoft.hiteen.admin.dto.AdminCashRuleUpdateRequest
import kr.jiasoft.hiteen.admin.infra.AdminCashRuleRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.cash.domain.CashRuleEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminCashRulesService(
    private val adminCashRuleRepository: AdminCashRuleRepository,
) {

    suspend fun list(
        search: String?,
        searchType: String?,
        status: String?,
        order: String?,
        currentPage: Int,
        perPage: Int,
    ): ApiPage<AdminCashRuleResponse> {
        val page = currentPage.coerceAtLeast(1)
        val size = perPage.coerceIn(1, 100)
        val offset = (page - 1) * size

        val normalizedSearch = search?.trim()?.takeIf { it.isNotBlank() }

        val normalizedSearchType = when (searchType?.trim()?.uppercase()) {
            null, "" -> "ALL"
            "ALL" -> "ALL"
            "ACTION_CODE" -> "ACTION_CODE"
            "DESCRIPTION" -> "DESCRIPTION"
            else -> "ALL"
        }

        // status: ACTIVE(기본)/DELETED/ALL
        val normalizedStatus = when (status?.trim()?.uppercase()) {
            null, "" -> "ACTIVE"
            "ACTIVE" -> "ACTIVE"
            "DELETED" -> "DELETED"
            "ALL" -> "ALL"
            else -> "ACTIVE"
        }

        val normalizedOrder = when (order?.trim()?.uppercase()) {
            "ASC" -> "ASC"
            else -> "DESC"
        }

        val total = adminCashRuleRepository.countList(
            search = normalizedSearch,
            searchType = normalizedSearchType,
            status = normalizedStatus,
        )
        val rows = adminCashRuleRepository.list(
            search = normalizedSearch,
            searchType = normalizedSearchType,
            status = normalizedStatus,
            order = normalizedOrder,
            limit = size,
            offset = offset,
        )

        return PageUtil.of(
            items = rows.map { it.toResponse() },
            total = total,
            page = page,
            size = size,
        )
    }

    suspend fun get(id: Long, includeDeleted: Boolean): AdminCashRuleResponse {
        val entity = adminCashRuleRepository.findByIdFiltered(id, includeDeleted)
            ?: throw IllegalStateException("캐시 정책을 찾을 수 없습니다. (id=$id)")
        return entity.toResponse()
    }

    @Transactional
    suspend fun create(request: AdminCashRuleCreateRequest): AdminCashRuleResponse {
        val actionCode = request.actionCode.trim()

        // DB가 action_code 전체 유니크라 soft delete 포함 중복은 불가. 사전에 메시지 개선.
        val existing = adminCashRuleRepository.findAnyByActionCode(actionCode)
        if (existing != null) {
            throw IllegalStateException("이미 존재하는 actionCode 입니다. (actionCode=$actionCode)")
        }

        val entity = CashRuleEntity(
            id = 0,
            actionCode = actionCode,
            amount = request.amount,
            dailyCap = request.dailyCap,
            cooldownSec = request.cooldownSec,
            description = request.description,
            deletedAt = null,
        )

        return adminCashRuleRepository.save(entity).toResponse()
    }

    @Transactional
    suspend fun update(id: Long, request: AdminCashRuleUpdateRequest): AdminCashRuleResponse {
        val current = adminCashRuleRepository.findById(id)
            ?: throw IllegalStateException("캐시 정책을 찾을 수 없습니다. (id=$id)")

        if (request.restore == true) {
            adminCashRuleRepository.restore(id)
        }

        val updated = current.copy(
            amount = request.amount ?: current.amount,
            dailyCap = request.dailyCap ?: current.dailyCap,
            cooldownSec = request.cooldownSec ?: current.cooldownSec,
            description = request.description ?: current.description,
            deletedAt = if (request.restore == true) null else current.deletedAt,
        )

        return adminCashRuleRepository.save(updated).toResponse()
    }

    @Transactional
    suspend fun delete(id: Long) {
        val updated = adminCashRuleRepository.softDelete(id)
        if (updated == 0) {
            // 멱등하게 처리: 이미 삭제됐거나 없는 경우
            val exists = adminCashRuleRepository.findById(id)
            if (exists == null) throw IllegalStateException("캐시 정책을 찾을 수 없습니다. (id=$id)")
        }
    }

    private fun CashRuleEntity.toResponse(): AdminCashRuleResponse = AdminCashRuleResponse(
        id = id,
        actionCode = actionCode,
        amount = amount,
        dailyCap = dailyCap,
        cooldownSec = cooldownSec,
        description = description,
        deletedAt = deletedAt,
    )
}

