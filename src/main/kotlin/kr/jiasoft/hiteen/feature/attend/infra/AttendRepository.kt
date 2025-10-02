package kr.jiasoft.hiteen.feature.attend.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.attend.domain.AttendEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface AttendRepository : CoroutineCrudRepository<AttendEntity, Long> {

    @Query("SELECT * FROM attends WHERE user_id = :userId ORDER BY attend_date DESC")
    fun findAllByUserId(userId: Long): Flow<AttendEntity>

    @Query("SELECT * FROM attends WHERE user_id = :userId AND attend_date = :date LIMIT 1")
    suspend fun findByUserIdAndAttendDate(userId: Long, date: LocalDate): AttendEntity?

    @Query("""
        WITH consecutive AS (
            SELECT attend_date,
                   ROW_NUMBER() OVER (ORDER BY attend_date DESC) AS rn
            FROM attends
            WHERE user_id = :userId
        )
        SELECT COUNT(*) 
        FROM consecutive
        WHERE attend_date = CURRENT_DATE - (rn - 1)
    """)
    suspend fun countConsecutiveAttendDays(userId: Long): Int

}
