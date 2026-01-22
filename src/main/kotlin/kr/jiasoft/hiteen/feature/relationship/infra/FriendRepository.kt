package kr.jiasoft.hiteen.feature.relationship.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.board.infra.CountProjection
import kr.jiasoft.hiteen.feature.relationship.domain.FriendEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

data class StatusProjection(
    val id: Long,
    val countStr: String?
)

@Repository
interface FriendRepository : CoroutineCrudRepository<FriendEntity, Long> {

    @Query("""
        SELECT COUNT(*)
        FROM friends
        WHERE (user_id = :userId OR friend_id = :userId)    
          AND status = 'ACCEPTED'
    """)
    suspend fun countFriendship(userId: Long): Int

    @Query("""
        SELECT u_id as id, COUNT(*)::int as count
        FROM (
            SELECT user_id as u_id FROM friends WHERE status = 'ACCEPTED' AND user_id IN (:userIds)
            UNION ALL
            SELECT friend_id as u_id FROM friends WHERE status = 'ACCEPTED' AND friend_id IN (:userIds)
        ) combined
        GROUP BY u_id
    """)
    fun countBulkFriendshipIn(userIds: List<Long>): Flow<CountProjection>

    @Query("""
        SELECT CASE WHEN user_id = :userId THEN friend_id ELSE user_id END AS friend_id
        FROM friends
        WHERE (user_id = :userId OR friend_id = :userId)    
          AND status = 'ACCEPTED'
    """)
    suspend fun findAllFriendship(userId: Long): Flow<Long>

    @Query("""
        SELECT COUNT(1) 
        FROM friends 
        WHERE (user_id = :userId AND friend_id = :targetId) 
           OR (user_id = :targetId AND friend_id = :userId)
    """)
    suspend fun existsFriend(userId: Long, targetId: Long): Long

    @Query("""
        SELECT status 
        FROM friends 
        WHERE (user_id = :userId AND friend_id = :targetId) 
           OR (user_id = :targetId AND friend_id = :userId)
    """)
    suspend fun findStatusFriend(userId: Long, targetId: Long): String

    @Query("""
        SELECT 
            CASE WHEN user_id = :currentUserId THEN friend_id ELSE user_id END as id,
            status as count_str
        FROM friends 
        WHERE (user_id = :currentUserId AND friend_id IN (:targetIds)) 
           OR (friend_id = :currentUserId AND user_id IN (:targetIds))
    """)
    fun findBulkStatusFriendIn(currentUserId: Long, targetIds: List<Long>): Flow<StatusProjection>

    /**
     * 특정 사용자와 여러 타겟 사용자들 간의 친구 관계 목록을 조회합니다.
     */
    @Query("""
        SELECT * FROM friends 
        WHERE (user_id = :currentUserId AND friend_id IN (:targetIds)) 
           OR (friend_id = :currentUserId AND user_id IN (:targetIds))
    """)
    fun findAllBetweenBulk(currentUserId: Long, targetIds: List<Long>): Flow<FriendEntity>

    // 두 사용자 사이의 관계 한 건(단일행 정책)
    @Query("""
        SELECT * FROM friends 
        WHERE (user_id = :a AND friend_id = :b) 
           OR (user_id = :b AND friend_id = :a)
        LIMIT 1
    """)
    suspend fun findBetween(a: Long, b: Long): FriendEntity?

    // 내가 맺은 친구들(수락됨)
    @Query("""
        SELECT * FROM friends 
        WHERE (user_id = :me OR friend_id = :me) 
          AND status = 'ACCEPTED'
        ORDER BY status_at DESC NULLS LAST, created_at DESC NULLS LAST
    """)
    suspend fun findAllAccepted(me: Long): Flow<FriendEntity>

    // 내가 보낸 대기중 요청(PENDING, 내가 요청자)
    @Query("""
        SELECT * FROM friends 
        WHERE user_id = :me AND status = 'PENDING'
        ORDER BY created_at DESC NULLS LAST
    """)
    suspend fun findAllOutgoingPending(me: Long): Flow<FriendEntity>

    // 내가 받은 대기중 요청(PENDING, 내가 수신자)
    @Query("""
        SELECT * FROM friends 
        WHERE friend_id = :me AND status = 'PENDING'
        ORDER BY created_at DESC NULLS LAST
    """)
    suspend fun findAllIncomingPending(me: Long): Flow<FriendEntity>


}
