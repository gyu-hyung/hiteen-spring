package kr.jiasoft.hiteen.feature.play.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.play.domain.GameEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface GameRepository : CoroutineCrudRepository<GameEntity, Long> {
    suspend fun findByCode(code: String): GameEntity?
    suspend fun existsByIdAndDeletedAtIsNull(id: Long): Boolean
    suspend fun findAllByDeletedAtIsNullOrderById(): Flow<GameEntity>
}