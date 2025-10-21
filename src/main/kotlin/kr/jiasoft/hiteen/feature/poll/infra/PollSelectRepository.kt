package kr.jiasoft.hiteen.feature.poll.infra

import kr.jiasoft.hiteen.feature.poll.domain.PollSelectEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import kotlinx.coroutines.flow.Flow

interface PollSelectRepository : CoroutineCrudRepository<PollSelectEntity, Long> {

    fun findAllByPollId(pollId: Long): Flow<PollSelectEntity>

    @Query("DELETE FROM poll_selects WHERE poll_id = :pollId")
    suspend fun deleteAllByPollId(pollId: Long)

    @Query("UPDATE poll_selects SET vote_count = vote_count + 1 WHERE id = :selectId")
    suspend fun increaseVoteCount(selectId: Long)


}
