package kr.jiasoft.hiteen.feature.interest.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.interest.domain.InterestMatchHistoryEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface InterestMatchHistoryRepository : CoroutineCrudRepository<InterestMatchHistoryEntity, Long> {
    suspend fun findByUserId(userId: Long): List<InterestMatchHistoryEntity>

    @Query("SELECT target_id FROM interest_match_history WHERE user_id = :userId")
    suspend fun findTargetIdsByUserId(userId: Long): Flow<Long>

    @Query("""
        SELECT COUNT(*) 
        FROM interest_match_history 
        WHERE user_id = :userId
          AND status = 'RECOMMENDED'
          AND created_at::date = CURRENT_DATE
    """)
    suspend fun countTodayRecommendations(userId: Long): Long

}