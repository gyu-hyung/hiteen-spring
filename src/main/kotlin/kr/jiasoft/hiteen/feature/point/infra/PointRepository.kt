package kr.jiasoft.hiteen.feature.point.infra

import kr.jiasoft.hiteen.feature.point.domain.PointEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDate

interface PointRepository : CoroutineCrudRepository<PointEntity, Long> {

    suspend fun findAllByUserId(userId: Long): List<PointEntity>

    @Query("SELECT SUM(p.point) FROM points p WHERE p.user_id = :userId AND p.deleted_at IS NULL")
    suspend fun sumPointsByUserId(userId: Long): Int?


    @Query("""
        SELECT * 
        FROM points 
        WHERE user_id = :userId
          AND DATE(created_at) BETWEEN :startDate AND :endDate
        ORDER BY created_at DESC
    """)
    suspend fun findAllByUserIdAndDateRange(
        userId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<PointEntity>

    @Query("""
        SELECT COUNT(*) 
        FROM points 
        WHERE user_id = :userId 
          AND pointable_type = :policyCode 
          AND DATE(created_at) = :today
    """)
    suspend fun countByUserAndPolicyAndDate(
        userId: Long,
        policyCode: String,
        today: LocalDate
    ): Int
}
