package kr.jiasoft.hiteen.feature.school.infra

import kr.jiasoft.hiteen.feature.school.domain.TimeTableEntity
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface TimeTableRepository : ReactiveCrudRepository<TimeTableEntity, Long> {
    suspend fun findByClassIdAndTimeDate(classId: Long, timeDate: LocalDateTime): List<TimeTableEntity>
}