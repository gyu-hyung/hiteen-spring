package kr.jiasoft.hiteen.feature.poll.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.board.infra.CountProjection
import kr.jiasoft.hiteen.feature.poll.domain.PollLikeEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PollLikeRepository : CoroutineCrudRepository<PollLikeEntity, Long> {
    suspend fun findByPollIdAndUserId(pollId: Long, userId: Long): PollLikeEntity?
    suspend fun deleteByPollIdAndUserId(pollId: Long, userId: Long)
    suspend fun countByPollId(pollId: Long): Long

    @Query("""
        SELECT poll_id as id, COUNT(*)::int as count
        FROM poll_likes
        WHERE poll_id IN (:pollIds)
        GROUP BY poll_id
    """)
    fun countBulkByPollIdIn(pollIds: List<Long>): Flow<CountProjection>

    @Query("""
        SELECT poll_id
        FROM poll_likes
        WHERE user_id = :userId AND poll_id IN (:pollIds)
    """)
    fun findAllIdsByUserIdAndPollIdIn(userId: Long, pollIds: List<Long>): Flow<Long>
}