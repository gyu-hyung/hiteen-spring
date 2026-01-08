package kr.jiasoft.hiteen.feature.interest.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.interest.domain.InterestMatchHistoryEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface InterestMatchHistoryRepository : CoroutineCrudRepository<InterestMatchHistoryEntity, Long> {
    suspend fun findByUserId(userId: Long): List<InterestMatchHistoryEntity>

    suspend fun findByUserIdAndTargetId(userId: Long, targetId: Long): InterestMatchHistoryEntity?

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


    @Query("""
        SELECT COUNT(*)
        FROM interest_match_history 
        WHERE user_id = :userId
          AND created_at >= NOW() - INTERVAL '24 hours'
    """)
    suspend fun countLast24HoursRecommendations(userId: Long): Long

    @Query("""
        SELECT * 
        FROM interest_match_history 
        WHERE user_id = :userId
        ORDER BY created_at DESC LIMIT 1
    """)
    suspend fun findLastRecommendations(userId: Long): InterestMatchHistoryEntity?

    @Query("""
        SELECT *
        FROM interest_match_history
        WHERE user_id = :userId
          AND status = 'RECOMMENDED'
          AND created_at >= (NOW() - INTERVAL '24 hours')
        ORDER BY created_at DESC
        LIMIT 1
    """)
    suspend fun findLatestRecommendationLast24Hours(userId: Long): InterestMatchHistoryEntity?



}