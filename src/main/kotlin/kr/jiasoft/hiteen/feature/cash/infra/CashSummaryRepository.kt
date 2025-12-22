package kr.jiasoft.hiteen.feature.cash.infra

import kr.jiasoft.hiteen.feature.cash.domain.CashSummaryEntity
import kr.jiasoft.hiteen.feature.point.domain.PointSummaryEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CashSummaryRepository : CoroutineCrudRepository<CashSummaryEntity, Long> {

    @Query("""
        INSERT INTO user_cash_summary (user_id, total_cash, updated_at)
        VALUES (:userId, :delta, now())
        ON CONFLICT (user_id)
        DO UPDATE SET total_cash = user_cash_summary.total_cash + EXCLUDED.total_cash,
                      updated_at = now()
    """)
    suspend fun upsertAddPoint(userId: Long, delta: Int)


    @Query("""
        SELECT * FROM user_cash_summary
        WHERE user_id = :userId
        LIMIT 1
    """)
    suspend fun findSummaryByUserId(userId: Long) : CashSummaryEntity?
}
