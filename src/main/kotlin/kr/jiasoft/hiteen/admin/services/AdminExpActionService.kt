package kr.jiasoft.hiteen.admin.services

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kr.jiasoft.hiteen.admin.dto.AdminExpActionCreateRequest
import kr.jiasoft.hiteen.admin.dto.AdminExpActionResponse
import kr.jiasoft.hiteen.admin.dto.AdminExpActionUpdateRequest
import kr.jiasoft.hiteen.feature.level.domain.ExpActionEntity
import kr.jiasoft.hiteen.feature.level.infra.ExpActionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminExpActionService(
    private val expActionRepository: ExpActionRepository,
) {

    fun list(enabled: Boolean? = null): Flow<AdminExpActionResponse> =
        expActionRepository.findAllByEnabled(enabled)
            .map { it.toResponse() }

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
