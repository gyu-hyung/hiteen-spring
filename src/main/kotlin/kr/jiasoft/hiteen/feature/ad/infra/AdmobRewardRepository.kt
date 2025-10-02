package kr.jiasoft.hiteen.feature.ad.infra

import kr.jiasoft.hiteen.feature.ad.domain.AdmobRewardEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AdmobRewardRepository : CoroutineCrudRepository<AdmobRewardEntity, Long> {
    suspend fun existsByTransactionId(transactionId: String): Boolean

    // 오늘(user 기준) 지급된 광고 리워드 수 (Postgres)
    @Query("""
        SELECT COUNT(*) FROM admob_rewards
        WHERE user_id = :userId
          AND DATE(created_at) = CURRENT_DATE
    """)
    suspend fun countTodayByUserId(userId: Long): Int


}
