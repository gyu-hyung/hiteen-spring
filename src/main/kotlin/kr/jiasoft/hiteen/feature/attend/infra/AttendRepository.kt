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
        WITH ordered AS (
            SELECT
                attend_date,
                ROW_NUMBER() OVER (ORDER BY attend_date DESC) AS rn
            FROM attends
            WHERE user_id = :userId
              AND attend_date <= CURRENT_DATE
        ),
        streak AS (
            SELECT
                attend_date
            FROM ordered
            WHERE attend_date = CURRENT_DATE - (rn - 1) * INTERVAL '1 day'
            ORDER BY attend_date DESC
            LIMIT 7
        )
        SELECT
            attend_date,
            ROW_NUMBER() OVER (ORDER BY attend_date ASC) AS rn
        FROM streak
        ORDER BY attend_date ASC
    """)
    fun findConsecutiveAttendDays(userId: Long): Flow<ConsecutiveAttendDay>

    /**
     * 단순 출석 현황용: 최근 7일(오늘 포함) 동안의 출석 날짜만 가져온다.
     * - DB CURRENT_DATE는 세션 타임존에 따라 달라질 수 있어, 서비스에서 KST today를 받아 사용한다.
     */
    @Query("""
        SELECT attend_date
        FROM attends
        WHERE user_id = :userId
          AND attend_date BETWEEN :startDate AND :endDate
        GROUP BY attend_date
        ORDER BY attend_date ASC
    """)
    fun findAttendDatesBetween(
        userId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<LocalDate>


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
