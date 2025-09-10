package kr.jiasoft.hiteen.feature.pin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.pin.domain.PinUsersEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PinUsersRepository : CoroutineCrudRepository<PinUsersEntity, Long> {
    suspend fun findAllByUserId(userId: Long): Flow<PinUsersEntity>
    suspend fun findAllByPinIdIn(pinIds: List<Long>): Flow<PinUsersEntity>
    suspend fun deleteAllByPinId(pinId: Long)
}