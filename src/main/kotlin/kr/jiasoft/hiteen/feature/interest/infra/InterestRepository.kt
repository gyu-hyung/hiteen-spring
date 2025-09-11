package kr.jiasoft.hiteen.feature.interest.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.interest.domain.InterestEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface InterestRepository : CoroutineCrudRepository<InterestEntity, Long> {
    suspend fun findByStatus(status: String): Flow<InterestEntity>
    @Query("SELECT * FROM interests ORDER BY id")
    fun findAllOrderById(): Flow<InterestEntity>
}