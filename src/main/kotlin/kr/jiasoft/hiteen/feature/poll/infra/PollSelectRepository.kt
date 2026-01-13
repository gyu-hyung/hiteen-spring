package kr.jiasoft.hiteen.feature.poll.infra

import kr.jiasoft.hiteen.feature.poll.domain.PollSelectEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.poll.dto.PollSelectResponse

interface PollSelectRepository : CoroutineCrudRepository<PollSelectEntity, Long> {

    fun findAllByPollId(pollId: Long): Flow<PollSelectEntity>

    @Query("""
        SELECT
            ps.id              AS id,
            ps.seq             AS seq,
            ps.content         AS content,
            ps.vote_count      AS vote_count,
            (
                SELECT psp.asset_uid
                FROM poll_select_photos psp
                WHERE psp.select_id = ps.id
                ORDER BY psp.seq ASC, psp.id ASC
                LIMIT 1
            )                  AS photos
        FROM poll_selects ps
        WHERE ps.poll_id = :pollId
        ORDER BY ps.seq ASC
    """)
    fun findSelectResponsesByPollId(
        pollId: Long
    ): Flow<PollSelectResponse>

    @Query("DELETE FROM poll_selects WHERE poll_id = :pollId")
    suspend fun deleteAllByPollId(pollId: Long)

    @Query("UPDATE poll_selects SET vote_count = vote_count + 1 WHERE id = :selectId")
    suspend fun increaseVoteCount(selectId: Long)


}
