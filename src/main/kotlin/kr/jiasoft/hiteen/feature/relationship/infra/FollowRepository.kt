package kr.jiasoft.hiteen.feature.relationship.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.relationship.domain.FollowEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository


@Repository
interface FollowRepository : CoroutineCrudRepository<FollowEntity, Long> {
    @Query("SELECT * FROM follows WHERE user_id = :userId AND follow_id = :followId")
    suspend fun findBetween(userId: Long, followId: Long): FollowEntity?
    suspend fun findAllByUserIdAndStatus(userId: Long, status: String): Flow<FollowEntity>
    suspend fun findAllByFollowIdAndStatus(followId: Long, status: String): Flow<FollowEntity>
    suspend fun countByFollowIdAndStatus(id: Long, name: String): Int
    suspend fun countByUserIdAndStatus(id: Long, name: String): Int

}
