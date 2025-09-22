package kr.jiasoft.hiteen.feature.timetable.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.timetable.domain.TimeUserEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface TimeUserRepository : CoroutineCrudRepository<TimeUserEntity, Long> {
    fun findAllByUserId(userId: Long): Flow<TimeUserEntity>
}
