package kr.jiasoft.hiteen.feature.relationship.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.board.infra.CountProjection
import kr.jiasoft.hiteen.feature.relationship.domain.FollowEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository


@Repository
interface FollowRepository : CoroutineCrudRepository<FollowEntity, Long> {

    suspend fun existsByUserIdAndFollowId(userId: Long, followId: Long): Boolean

    @Query("""
        SELECT user_id
        FROM follows
        WHERE follow_id = :userId
          AND status = 'ACCEPTED'
    """)
    suspend fun findAllFollowerIds(userId: Long): Flow<Long>

    @Query("SELECT * FROM follows WHERE user_id = :userId AND follow_id = :followId")
    suspend fun findBetween(userId: Long, followId: Long): FollowEntity?
    suspend fun findAllByUserIdAndStatus(userId: Long, status: String): Flow<FollowEntity>
    suspend fun findAllByFollowIdAndStatus(followId: Long, status: String): Flow<FollowEntity>
    suspend fun countByFollowIdAndStatus(id: Long, name: String): Int
    suspend fun countByUserIdAndStatus(id: Long, name: String): Int

    @Query("""
        SELECT follow_id as id, COUNT(*)::int as count
        FROM follows
        WHERE follow_id IN (:userIds) AND status = :status
        GROUP BY follow_id
    """)
    fun countBulkFollowersIn(userIds: List<Long>, status: String): Flow<CountProjection>

    @Query("""
        SELECT user_id as id, COUNT(*)::int as count
        FROM follows
        WHERE user_id IN (:userIds) AND status = :status
        GROUP BY user_id
    """)
    fun countBulkFollowingIn(userIds: List<Long>, status: String): Flow<CountProjection>

    @Query("SELECT COUNT(*) FROM follows WHERE user_id = :followerId AND follow_id = :targetId")
    suspend fun existsFollow(followerId: Long, targetId: Long): Long

    @Query("SELECT status FROM follows WHERE user_id = :followerId AND follow_id = :targetId")
    suspend fun findStatusFollow(followerId: Long, targetId: Long): String?

    @Query("""
        SELECT follow_id as id, status as count_str
        FROM follows
        WHERE user_id = :currentUserId AND follow_id IN (:targetIds)
    """)
    fun findBulkStatusFollowIn(currentUserId: Long, targetIds: List<Long>): Flow<StatusProjection>

}
