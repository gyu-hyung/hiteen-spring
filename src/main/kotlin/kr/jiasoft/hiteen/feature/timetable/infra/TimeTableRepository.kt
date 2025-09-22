package kr.jiasoft.hiteen.feature.timetable.infra

import kr.jiasoft.hiteen.feature.timetable.domain.TimeTableEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDate

interface TimeTableRepository : CoroutineCrudRepository<TimeTableEntity, Long> {
    @Modifying
    @Query(
        """
        INSERT INTO time_table (class_id, year, semester, time_date, period, subject, created_at, updated_at)
        VALUES (:classId, :year, :semester, :timeDate, :period, :subject, now(), now())
        ON CONFLICT (class_id, time_date, period)
        DO UPDATE SET subject = EXCLUDED.subject,
                      year = EXCLUDED.year,
                      semester = EXCLUDED.semester,
                      updated_at = now()
        """
    )
    suspend fun upsert(
        classId: Long,
        year: Int,
        semester: Int,
        timeDate: LocalDate,
        period: Int,
        subject: String
    ): Int

}
