package kr.jiasoft.hiteen.feature.timetable.infra

import kr.jiasoft.hiteen.feature.timetable.domain.TimeTableImageEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface TimeTableImageRepository : CoroutineCrudRepository<TimeTableImageEntity, Long> {
    suspend fun findFirstByClassIdAndSemesterOrderByCreatedAtDesc(classId: Long, semester: Int): TimeTableImageEntity?
}
