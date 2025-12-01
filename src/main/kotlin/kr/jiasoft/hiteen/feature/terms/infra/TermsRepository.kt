package kr.jiasoft.hiteen.feature.terms.infra

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import kr.jiasoft.hiteen.feature.terms.domain.TermsEntity
import java.util.UUID

@Repository
interface TermsRepository : CoroutineCrudRepository<TermsEntity, Long> {

    suspend fun findByUidAndStatus(uid: UUID, status: Short): TermsEntity?

    fun findAllByCategoryAndStatusOrderBySortAsc(category: String, status: Short): Flow<TermsEntity>
}
