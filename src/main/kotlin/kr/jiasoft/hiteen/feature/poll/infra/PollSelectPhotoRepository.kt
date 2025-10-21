package kr.jiasoft.hiteen.feature.poll.infra

import kr.jiasoft.hiteen.feature.poll.domain.PollSelectPhotoEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query

interface PollSelectPhotoRepository : CoroutineCrudRepository<PollSelectPhotoEntity, Long> {
    fun findAllBySelectId(selectId: Long): Flow<PollSelectPhotoEntity>

    @Query("""
    DELETE FROM poll_select_photos 
    WHERE select_id IN (SELECT id FROM poll_selects WHERE poll_id = :pollId)
    """)
    suspend fun deleteAllByPollId(pollId: Long)
}
