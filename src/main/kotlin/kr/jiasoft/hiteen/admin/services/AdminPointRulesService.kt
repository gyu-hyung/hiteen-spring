package kr.jiasoft.hiteen.admin.services

import kr.jiasoft.hiteen.admin.dto.AdminPointRuleCreateRequest
import kr.jiasoft.hiteen.admin.dto.AdminPointRuleResponse
import kr.jiasoft.hiteen.admin.dto.AdminPointRuleUpdateRequest
import kr.jiasoft.hiteen.admin.infra.AdminPointRuleRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.point.domain.PointRuleEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminPointRulesService(
    private val adminPointRuleRepository: AdminPointRuleRepository,
) {

    suspend fun list(
        search: String?,
        searchType: String?,
        status: String?,
        order: String?,
        currentPage: Int,
        perPage: Int,
    ): ApiPage<AdminPointRuleResponse> {
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

        val total = adminPointRuleRepository.countList(
            search = normalizedSearch,
            searchType = normalizedSearchType,
            status = normalizedStatus,
        )
        val rows = adminPointRuleRepository.list(
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

    suspend fun get(id: Long, includeDeleted: Boolean): AdminPointRuleResponse {
        val entity = adminPointRuleRepository.findByIdFiltered(id, includeDeleted)
            ?: throw IllegalStateException("포인트 정책을 찾을 수 없습니다. (id=$id)")
        return entity.toResponse()
    }

    @Transactional
    suspend fun create(request: AdminPointRuleCreateRequest): AdminPointRuleResponse {
        val actionCode = request.actionCode.trim()

        // DB가 action_code 전체 유니크라 soft delete 포함 중복은 불가. 사전에 메시지 개선.
        val existing = adminPointRuleRepository.findAnyByActionCode(actionCode)
        if (existing != null) {
            throw IllegalStateException("이미 존재하는 actionCode 입니다. (actionCode=$actionCode)")
        }

        val entity = PointRuleEntity(
            id = 0,
            actionCode = actionCode,
            point = request.point,
            dailyCap = request.dailyCap,
            cooldownSec = request.cooldownSec,
            description = request.description,
            deletedAt = null,
        )

        return adminPointRuleRepository.save(entity).toResponse()
    }

    @Transactional
    suspend fun update(id: Long, request: AdminPointRuleUpdateRequest): AdminPointRuleResponse {
        val current = adminPointRuleRepository.findById(id)
            ?: throw IllegalStateException("포인트 정책을 찾을 수 없습니다. (id=$id)")

        if (request.restore == true) {
            adminPointRuleRepository.restore(id)
        }

        val updated = current.copy(
            point = request.point ?: current.point,
            dailyCap = request.dailyCap ?: current.dailyCap,
            cooldownSec = request.cooldownSec ?: current.cooldownSec,
            description = request.description ?: current.description,
            deletedAt = if (request.restore == true) null else current.deletedAt,
        )

        return adminPointRuleRepository.save(updated).toResponse()
    }

    @Transactional
    suspend fun delete(id: Long) {
        val updated = adminPointRuleRepository.softDelete(id)
        if (updated == 0) {
            // 멱등하게 처리: 이미 삭제됐거나 없는 경우
            val exists = adminPointRuleRepository.findById(id)
            if (exists == null) throw IllegalStateException("포인트 정책을 찾을 수 없습니다. (id=$id)")
        }
    }

    private fun PointRuleEntity.toResponse(): AdminPointRuleResponse = AdminPointRuleResponse(
        id = id,
        actionCode = actionCode,
        point = point,
        dailyCap = dailyCap,
        cooldownSec = cooldownSec,
        description = description,
        deletedAt = deletedAt,
    )
}
