package kr.jiasoft.hiteen.feature.interest.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.interest.domain.InterestEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface InterestRepository : CoroutineCrudRepository<InterestEntity, Long> {
    suspend fun findByStatus(status: String): Flow<InterestEntity>

    @Query("SELECT * FROM interests ORDER BY category, id")
    fun findAllOrderByCategoryAndId(): Flow<InterestEntity>

    @Query("SELECT * FROM interests WHERE category IN ('추천방식', '추천옵션', '추천제외')")
    fun findAllSystemCategory(): Flow<InterestEntity>

    suspend fun findByCategoryAndTopicIn(category: String, topic: List<String>): Flow<InterestEntity>

    @Query(
        """
        SELECT i.id,
               i.topic,
               i.category,
               CASE WHEN iu.id IS NULL THEN 'N' ELSE 'Y' END AS status,
               i.created_at,
               i.updated_at,
               i.deleted_at,
               i.created_id,
               i.updated_id,
               i.deleted_id
        FROM interests i
        LEFT JOIN interest_user iu
          ON i.id = iu.interest_id
         AND iu.user_id = :userId
        ORDER BY i.category, i.id
        """
    )
    fun findAllWithUserStatus(userId: Long): Flow<InterestEntity>

}