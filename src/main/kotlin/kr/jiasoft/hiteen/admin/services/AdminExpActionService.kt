package kr.jiasoft.hiteen.admin.services

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminExpActionCreateRequest
import kr.jiasoft.hiteen.admin.dto.AdminExpActionResponse
import kr.jiasoft.hiteen.admin.dto.AdminExpActionUpdateRequest
import kr.jiasoft.hiteen.admin.infra.AdminExpActionRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.level.domain.ExpActionEntity
import kr.jiasoft.hiteen.feature.level.infra.ExpActionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminExpActionService(
    private val expActionRepository: ExpActionRepository,
    private val adminExpActionRepository: AdminExpActionRepository,
) {

    suspend fun listExpActions(
        status: Boolean?,
        searchType: String?,
        search: String?,
        order: String,
        size: Int,
        page: Int,
    ): ApiPage<AdminExpActionResponse> {
        val page = page.coerceAtLeast(1)
        val size = size.coerceIn(1, 100)
        val offset = (page - 1) * size

        // 총 레코드수
        val total = adminExpActionRepository.totalCount(status, searchType, search)
        val items = adminExpActionRepository.listByPage(status, searchType, search, order, size, offset).toList()

        return PageUtil.of(items, total, page, size)
    }

    // 목록
    suspend fun list(enabled: Boolean? = null): Flow<AdminExpActionResponse> =
        expActionRepository.findAllByEnabled(enabled)
            .map { it.toResponse() }

    fun listByPage(
        enabled: Boolean? = null,
        page: Int = 1,
        size: Int = 10,
        order: String = "DESC",
    ): Flow<AdminExpActionResponse> {
        val safePage = if (page <= 0) 1 else page
        val safeSize = if (size <= 0) 10 else size
        val safeOrder = order.uppercase()
        val offset = ((safePage - 1L) * safeSize).coerceAtLeast(0)

        return expActionRepository.listByPage(
            enabled = enabled,
            order = safeOrder,
            size = safeSize,
            offset = offset,
        ).map { it.toResponse() }
    }

    // 전체 갯수
    suspend fun totalCount(enabled: Boolean? = null): Int =
        expActionRepository.totalCount(enabled)

    suspend fun get(actionCode: String): AdminExpActionResponse {
        val entity = expActionRepository.findByActionCode(actionCode)
            ?: throw IllegalArgumentException("존재하지 않는 액션 코드: $actionCode")
        return entity.toResponse()
    }

    @Transactional
    suspend fun create(request: AdminExpActionCreateRequest): AdminExpActionResponse {
        val actionCode = request.actionCode?.trim()?.uppercase()
            ?: throw IllegalArgumentException("actionCode는 필수입니다.")

        if (expActionRepository.findByActionCode(actionCode) != null) {
            throw IllegalArgumentException("이미 존재하는 actionCode 입니다: $actionCode")
        }

        val entity = ExpActionEntity(
            actionCode = actionCode,
            description = request.description?.trim() ?: "-",
            points = request.points ?: 0,
            dailyLimit = request.dailyLimit,
            enabled = request.enabled ?: true,
        )

        val saved = expActionRepository.save(entity)
        return saved.toResponse()
    }

    @Transactional
    suspend fun update(actionCode: String, request: AdminExpActionUpdateRequest): AdminExpActionResponse {
        val existing = expActionRepository.findByActionCode(actionCode)
            ?: throw IllegalArgumentException("존재하지 않는 액션 코드: $actionCode")

        val updated = existing.copy(
            description = request.description?.trim() ?: existing.description,
            points = request.points ?: existing.points,
            dailyLimit = request.dailyLimit,
            enabled = request.enabled ?: existing.enabled,
        )

        val saved = expActionRepository.save(updated)
        return saved.toResponse()
    }

    @Transactional
    suspend fun disable(actionCode: String) {
        val affected = expActionRepository.disableByActionCode(actionCode)
        if (affected < 1) throw IllegalArgumentException("존재하지 않는 액션 코드: $actionCode")
    }

    private fun ExpActionEntity.toResponse(): AdminExpActionResponse = AdminExpActionResponse(
        actionCode = actionCode,
        description = description,
        points = points,
        dailyLimit = dailyLimit,
        enabled = enabled,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
