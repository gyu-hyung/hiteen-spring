package kr.jiasoft.hiteen.feature.cash.infra

import kr.jiasoft.hiteen.feature.cash.domain.CashEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDate

interface CashRepository : CoroutineCrudRepository<CashEntity, Long> {

    suspend fun findAllByUserId(userId: Long): List<CashEntity>

    @Query("SELECT SUM(c.amount) FROM cash c WHERE c.user_id = :userId AND c.deleted_at IS NULL")
    suspend fun sumCashByUserId(userId: Long): Int?


    @Query("""
        SELECT * 
        FROM cash 
        WHERE user_id = :userId
          AND DATE(created_at) BETWEEN :startDate AND :endDate
        ORDER BY created_at DESC
    """)
    suspend fun findAllByUserIdAndDateRange(
        userId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<CashEntity>

    @Query("""
        SELECT COUNT(*) 
        FROM cash 
        WHERE user_id = :userId 
          AND cashable_type = :policyCode 
          AND DATE(created_at) = :today
    """)
    suspend fun countByUserAndPolicyAndDate(
        userId: Long,
        policyCode: String,
        today: LocalDate
    ): Int

    /**
     * 특정 정책으로 지급된 캐시 건수 조회 (이벤트 제한용)
     */
    @Query("""SELECT COUNT(*) FROM cash WHERE cashable_type = :policyCode""")
    suspend fun countByPolicy(policyCode: String): Long
}
