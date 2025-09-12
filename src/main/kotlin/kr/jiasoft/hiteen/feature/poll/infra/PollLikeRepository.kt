package kr.jiasoft.hiteen.feature.poll.infra

import kr.jiasoft.hiteen.feature.poll.domain.PollLikeEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PollLikeRepository : CoroutineCrudRepository<PollLikeEntity, Long> {
    suspend fun findByPollIdAndUserId(boardId: Long, userId: Long): PollLikeEntity?
    suspend fun deleteByPollIdAndUserId(boardId: Long, userId: Long)
    suspend fun countByPollId(boardId: Long): Long
}