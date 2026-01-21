package kr.jiasoft.hiteen.feature.poll.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.poll.domain.PollPhotoEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface PollPhotoRepository : CoroutineCrudRepository<PollPhotoEntity, Long> {
    fun findAllByPollId(pollId: Long): Flow<PollPhotoEntity>

    @Query("""
        SELECT *
        FROM poll_photos
        WHERE poll_id = ANY(:pollIds)
        ORDER BY poll_id ASC, seq ASC, id ASC
    """)
    fun findAllByPollIdIn(pollIds: Array<Long>): Flow<PollPhotoEntity>

    suspend fun deleteAllByPollId(pollId: Long)
}
