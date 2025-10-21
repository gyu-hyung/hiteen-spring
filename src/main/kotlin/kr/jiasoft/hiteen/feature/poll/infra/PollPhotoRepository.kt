package kr.jiasoft.hiteen.feature.poll.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.poll.domain.PollPhotoEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface PollPhotoRepository : CoroutineCrudRepository<PollPhotoEntity, Long> {
    fun findAllByPollId(pollId: Long): Flow<PollPhotoEntity>


    suspend fun deleteAllByPollId(pollId: Long)
}
