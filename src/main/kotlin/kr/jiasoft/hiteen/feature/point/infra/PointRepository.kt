package kr.jiasoft.hiteen.feature.point.infra

import kr.jiasoft.hiteen.feature.point.domain.PointEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface PointRepository : CoroutineCrudRepository<PointEntity, Long> {
    suspend fun findAllByUserId(userId: Long): List<PointEntity>
}
