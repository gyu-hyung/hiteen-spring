package kr.jiasoft.hiteen.feature.level.infra

import kr.jiasoft.hiteen.feature.level.domain.UserExpHistoryEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface UserExpHistoryRepository : CoroutineCrudRepository<UserExpHistoryEntity, Long> {

    @Query("""
        SELECT COUNT(*) 
        FROM user_exp_history 
        WHERE user_id = :userId 
          AND action_code = :actionCode 
    """)
    suspend fun count(userId: Long, actionCode: String): Int


    @Query("""
        SELECT COUNT(*) 
        FROM user_exp_history 
        WHERE user_id = :userId 
          AND action_code = :actionCode 
          AND DATE(created_at) = :today
    """)
    suspend fun countToday(userId: Long, actionCode: String, today: LocalDate): Int


    @Query("""
        SELECT COUNT(*) 
        FROM user_exp_history 
        WHERE user_id = :userId 
          AND action_code = :actionCode 
          AND target_id = :targetId
          AND DATE(created_at) = :today
    """)
    suspend fun countToday(userId: Long, actionCode: String, targetId: Long, today: LocalDate): Int

    @Query("""
        SELECT EXISTS (
            SELECT 1 FROM user_exp_history
            WHERE user_id = :userId
              AND action_code = :actionCode
              AND target_id = :targetId
              AND DATE(created_at) = :today
        )
    """)
    suspend fun existsTargetIdAndToday(userId: Long, actionCode: String, targetId: Long, today: LocalDate): Boolean


    @Query("""
        SELECT EXISTS (
            SELECT 1 FROM user_exp_history
            WHERE action_code = :actionCode
              AND user_id = :userId 
        )
    """)
    suspend fun exists(userId: Long, actionCode: String): Boolean



    @Query("""
        SELECT EXISTS (
            SELECT 1 FROM user_exp_history
            WHERE action_code = :actionCode
              AND user_id = :userId 
              AND target_id = :targetId
        )
    """)
    suspend fun existsTargetId(userId: Long, actionCode: String, targetId: Long): Boolean


    @Query("""
    SELECT COALESCE(SUM(points), 0)
    FROM user_exp_history
    WHERE user_id = :userId
      AND action_code = :actionCode
      AND DATE(created_at) = :date
""")
    suspend fun sumToday(
        userId: Long,
        actionCode: String,
        date: LocalDate
    ): Int?

}
