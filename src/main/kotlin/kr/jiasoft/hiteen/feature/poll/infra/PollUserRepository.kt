package kr.jiasoft.hiteen.feature.poll.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.board.infra.CountProjection
import kr.jiasoft.hiteen.feature.poll.domain.PollUserEntity
import kr.jiasoft.hiteen.feature.poll.dto.VoteCountRow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PollUserRepository : CoroutineCrudRepository<PollUserEntity, Long> {


    @Query("SELECT * FROM poll_users WHERE poll_id = :pollId AND user_id = :userId LIMIT 1")
    suspend fun findByPollIdAndUserId(pollId: Long, userId: Long): PollUserEntity?

    @Query("""
        SELECT seq, COUNT(*) as votes
        FROM poll_users
        WHERE poll_id = :pollId
        GROUP BY seq
    """)
    fun countVotesByPollId(pollId: Long): Flow<VoteCountRow>

    suspend fun countByPollId(pollId: Long) : Int

    @Query("""
        SELECT COUNT(*)        
        FROM poll_users pu
        LEFT JOIN polls p ON pu.poll_id = p.id 
        WHERE p.deleted_at is null AND pu.user_id = :userId
    """)
    suspend fun countByUserIdAndDeletedAtIsNull(userId: Long) : Int

    @Query("""
        SELECT pu.user_id as id, COUNT(*)::int as count
        FROM poll_users pu
        LEFT JOIN polls p ON pu.poll_id = p.id 
        WHERE p.deleted_at IS NULL AND pu.user_id IN (:userIds)
        GROUP BY pu.user_id
    """)
    fun countBulkByUserIdIn(userIds: List<Long>): Flow<CountProjection>

    @Query("""
        SELECT *
        FROM poll_users
        WHERE user_id = :userId AND poll_id IN (:pollIds)
    """)
    fun findAllByUserIdAndPollIdIn(userId: Long, pollIds: List<Long>): Flow<PollUserEntity>

}