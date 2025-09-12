package kr.jiasoft.hiteen.feature.poll.infra

import kr.jiasoft.hiteen.feature.poll.domain.PollUserEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PollUserRepository : CoroutineCrudRepository<PollUserEntity, Long> {

    @Query("SELECT * FROM poll_users WHERE poll_id = :pollId AND user_id = :userId LIMIT 1")
    suspend fun findByPollIdAndUserId(pollId: Long, userId: Long): PollUserEntity?

}