package kr.jiasoft.hiteen.feature.relationship.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.relationship.domain.FollowEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository


@Repository
interface FollowRepository : CoroutineCrudRepository<FollowEntity, Long> {

    // 두 사용자 사이의 관계 한 건(단일행 정책)
    @Query("""
        SELECT * FROM follows 
        WHERE (user_id = :a AND follow_id = :b) 
           OR (user_id = :b AND follow_id = :a)
        LIMIT 1
    """)
    suspend fun findBetween(a: Long, b: Long): FollowEntity?

    // 내가 맺은 친구들(수락됨)
    @Query("""
        SELECT * FROM follows 
        WHERE (user_id = :me OR follow_id = :me) 
          AND status = 'ACCEPTED'
        ORDER BY status_at DESC NULLS LAST, created_at DESC NULLS LAST
    """)
    suspend fun findAllAccepted(me: Long): Flow<FollowEntity>

    // 내가 보낸 대기중 요청(PENDING, 내가 요청자)
    @Query("""
        SELECT * FROM follows 
        WHERE user_id = :me AND status = 'PENDING'
        ORDER BY created_at DESC NULLS LAST
    """)
    suspend fun findAllOutgoingPending(me: Long): Flow<FollowEntity>

    // 내가 받은 대기중 요청(PENDING, 내가 수신자)
    @Query("""
        SELECT * FROM follows 
        WHERE follow_id = :me AND status = 'PENDING'
        ORDER BY created_at DESC NULLS LAST
    """)
    suspend fun findAllIncomingPending(me: Long): Flow<FollowEntity>
}
