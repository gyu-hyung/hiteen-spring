package kr.jiasoft.hiteen.feature.attend.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.attend.domain.AttendEntity
import kr.jiasoft.hiteen.feature.session.dto.ConsecutiveAttendDay
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
    WITH sorted AS (
        SELECT attend_date,
               ROW_NUMBER() OVER (ORDER BY attend_date) AS rn
        FROM attends
        WHERE user_id = :userId
    ),
    grouped AS (
        SELECT attend_date,
               rn,
               attend_date - (rn * INTERVAL '1 day') AS grp
        FROM sorted
    ),
    recent_chain AS (
        SELECT attend_date
        FROM grouped
        WHERE grp = (SELECT grp FROM grouped ORDER BY attend_date DESC LIMIT 1)
    )
    SELECT COUNT(*) % 7 AS streak_mod
    FROM recent_chain
    """)
    suspend fun countConsecutiveAttendDays(userId: Long): Int


    @Query("""
            WITH sorted AS (
                SELECT attend_date,
                       ROW_NUMBER() OVER (ORDER BY attend_date) AS rn
                FROM attends
                WHERE user_id = :userId
            ),
            grouped AS (
                SELECT attend_date,
                       rn,
                       attend_date - (rn * INTERVAL '1 day') AS grp
                FROM sorted
            ),
            recent_chain AS (
                SELECT attend_date
                FROM grouped
                WHERE grp = (SELECT grp FROM grouped ORDER BY attend_date DESC LIMIT 1)
            ),
            counter AS (
                SELECT COUNT(*) % 7 AS streak_mod FROM recent_chain
            )
            SELECT attend_date,
                   ROW_NUMBER() OVER (ORDER BY attend_date) AS rn
            FROM (
                SELECT attend_date
                FROM recent_chain, counter
                WHERE counter.streak_mod > 0
                ORDER BY attend_date DESC
                LIMIT (SELECT streak_mod FROM counter)
            ) AS limited
            ORDER BY attend_date ASC
        """)
    fun findConsecutiveAttendDays(userId: Long): Flow<ConsecutiveAttendDay>


    fun findAllByUserIdOrderByIdDesc(userId: Long): Flow<AttendEntity>


    @Query("""
        SELECT *
        FROM attends
        WHERE user_id = :userId
          AND (
                :cursorDate IS NULL
                OR (
                    attend_date < :cursorDate
                    OR (attend_date = :cursorDate AND id < :cursorId)
                )
          )
        ORDER BY attend_date DESC, id DESC
        LIMIT :limit
    """)
    fun findByCursor(
        userId: Long,
        cursorDate: LocalDate?,
        cursorId: Long?,
        limit: Int
    ): Flow<AttendEntity>


}
