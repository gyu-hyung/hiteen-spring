package kr.jiasoft.hiteen.feature.poll.infra

import kotlinx.coroutines.flow.Flow
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
    suspend fun countByUserId(userId: Long) : Int

}