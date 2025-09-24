package kr.jiasoft.hiteen.feature.interest.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.interest.domain.InterestUserEntity
import kr.jiasoft.hiteen.feature.interest.dto.InterestUserResponse
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface InterestUserRepository : CoroutineCrudRepository<InterestUserEntity, Long> {

    suspend fun findByUserId(userId: Long): Flow<InterestUserEntity>

    suspend fun findByUserIdAndInterestId(userId: Long, interestId: Long): InterestUserEntity?

    suspend fun deleteByUserIdAndInterestId(userId: Long, interestId: Long): Long

    @Query("""
        SELECT
          (SELECT u.uid FROM users u WHERE u.id = iu.user_id) AS user_uid,
          i.id, i.topic, i.category, i.status,
          iu.*
        FROM interest_user iu
        JOIN interests i ON iu.interest_id = i.id
        WHERE (:id IS NULL OR iu.id = :id)
          AND (:userId IS NULL OR iu.user_id = :userId)
    """)
    suspend fun getInterestResponseById(id: Long?, userId: Long?): Flow<InterestUserResponse>


    @Query("""
        SELECT DISTINCT iu.user_id
        FROM interest_user iu
        WHERE iu.interest_id IN (:interestIds)
        AND iu.user_id <> :currentUserId
        AND iu.user_id IN (
            SELECT user_id
            FROM user_photos
            GROUP BY user_id
            HAVING COUNT(*) >= 3
        )
        AND user_id NOT IN (
            SELECT follow_id FROM follows WHERE status = 'ACCEPTED' AND user_id = :currentUserId
        )
        AND user_id NOT IN (
            SELECT CASE WHEN :currentUserId = user_id THEN friend_id ELSE user_id END FROM friends WHERE (user_id = :currentUserId OR friend_id = :currentUserId)
        )
    """)
    fun findUsersByInterestIds(interestIds: Set<Long>, currentUserId: Long): Flow<Long>




}
