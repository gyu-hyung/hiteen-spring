package kr.jiasoft.hiteen.feature.pin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.pin.domain.PinEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PinRepository : CoroutineCrudRepository<PinEntity, Long> {
    /** 특정 사용자가 등록한 핀 목록 */
    fun findAllByUserId(userId: Long): Flow<PinEntity>

    /** 사용자에게 공개된 핀 목록 */
    fun findAllByVisibilityOrderByIdDesc(visibility: String): Flow<PinEntity>
}