package kr.jiasoft.hiteen.feature.terms.app

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.terms.domain.TermsEntity
import kr.jiasoft.hiteen.feature.terms.dto.TermsCreateRequest
import kr.jiasoft.hiteen.feature.terms.dto.TermsResponse
import kr.jiasoft.hiteen.feature.terms.dto.TermsUpdateRequest
import kr.jiasoft.hiteen.feature.terms.infra.TermsRepository
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
class TermsService(
    private val repo: TermsRepository
) {

    suspend fun create(req: TermsCreateRequest): TermsResponse {
        val saved = repo.save(
            TermsEntity(
                uid = UUID.randomUUID(),
                category = req.category,
                code = req.code,
                version = req.version,
                title = req.title,
                content = req.content,
                sort = req.sort,
                isRequired = req.isRequired,
                status = req.status,
                createdId = req.createdId
            )
        )
        return TermsResponse.from(saved)
    }

    suspend fun get(uid: UUID, plain: Boolean = false): TermsResponse {
        val entity = repo.findByUidAndStatus(uid, 1)
            ?: throw IllegalArgumentException("약관을 찾을 수 없습니다.")

        return if (plain) TermsResponse.toPlainText(entity) else TermsResponse.from(entity)
    }

    suspend fun getActiveList(category: String = "Agreement"): List<TermsResponse> =
        repo.findAllByCategoryAndStatusOrderBySortAsc(category, 1)
            .map { TermsResponse.from(it) }
            .toList()

    suspend fun update(uid: UUID, req: TermsUpdateRequest): TermsResponse {
        val existing = repo.findByUid(uid)
            ?: throw IllegalArgumentException("약관을 찾을 수 없습니다.")

        val updated = existing.copy(
            category = req.category,
            code = req.code,
            version = req.version,
            title = req.title,
            content = req.content,
            sort = req.sort,
            isRequired = req.isRequired,
            status = req.status,
            updatedId = req.updatedId,
            updatedAt = OffsetDateTime.now(),
        )

        val saved = repo.save(updated)
        return TermsResponse.from(saved)
    }

    /**
     * 소프트 삭제: status=0 + deleted 정보 기록
     */
    suspend fun delete(uid: UUID, deletedId: Long) {
        val existing = repo.findByUid(uid)
            ?: throw IllegalArgumentException("약관을 찾을 수 없습니다.")

        val deleted = existing.copy(
            status = 0,
            deletedId = deletedId,
            deletedAt = OffsetDateTime.now(),
            updatedId = deletedId,
            updatedAt = OffsetDateTime.now(),
        )

        repo.save(deleted)
    }
}
